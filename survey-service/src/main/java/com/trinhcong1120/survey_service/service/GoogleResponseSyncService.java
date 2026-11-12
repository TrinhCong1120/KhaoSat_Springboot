package com.trinhcong1120.survey_service.service;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.model.FormResponse;
import com.google.api.services.forms.v1.model.ListFormResponsesResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class GoogleResponseSyncService {

    private static final Logger log = LoggerFactory.getLogger(GoogleResponseSyncService.class);

    private final SurveyRepository surveyRepo;
    private final SurveyIntegrationRepository integrationRepo;
    private final GoogleConnectionRepository connectionRepo;
    private final GoogleQuestionMappingRepository mappingRepo;
    private final ExternalSurveyResponseRepository externalResponseRepo;
    private final ResponseRepository responseRepo;
    private final AnswerRepository answerRepo;
    private final OptionRepository optionRepo;
    private final QuestionRepository questionRepo;
    private final GoogleFormsService formsService;

    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

    public GoogleResponseSyncService(
            SurveyRepository surveyRepo,
            SurveyIntegrationRepository integrationRepo,
            GoogleConnectionRepository connectionRepo,
            GoogleQuestionMappingRepository mappingRepo,
            ExternalSurveyResponseRepository externalResponseRepo,
            ResponseRepository responseRepo,
            AnswerRepository answerRepo,
            OptionRepository optionRepo,
            QuestionRepository questionRepo,
            GoogleFormsService formsService
    ) {
        this.surveyRepo = surveyRepo;
        this.integrationRepo = integrationRepo;
        this.connectionRepo = connectionRepo;
        this.mappingRepo = mappingRepo;
        this.externalResponseRepo = externalResponseRepo;
        this.responseRepo = responseRepo;
        this.answerRepo = answerRepo;
        this.optionRepo = optionRepo;
        this.questionRepo = questionRepo;
        this.formsService = formsService;
    }

    public record SyncResult(
            UUID surveyId,
            int syncedCount,
            int totalExternalCount,
            String status,
            String message,
            boolean isAcceptingResponses
    ) {}

    public SyncResult syncResponses(UUID surveyId, boolean manualTrigger) {
        Survey survey = surveyRepo.findById(surveyId)
                .orElseThrow(() -> new BadRequestException("Khảo sát không tồn tại"));

        SurveyIntegration integration = integrationRepo.findBySurveyId(surveyId)
                .orElseThrow(() -> new BadRequestException("Khảo sát này chưa liên kết với Google Forms"));

        GoogleConnection ownerConnection = null;
        if (integration.getOwnerConnectionId() != null) {
            ownerConnection = connectionRepo.findById(integration.getOwnerConnectionId()).orElse(null);
        }
        if (ownerConnection == null || !"ACTIVE".equals(ownerConnection.getStatus())) {
            if (survey.getCreatorUserId() != null) {
                ownerConnection = connectionRepo.findByUserId(survey.getCreatorUserId()).stream()
                        .filter(c -> "ACTIVE".equals(c.getStatus()))
                        .findFirst()
                        .orElse(null);
            }
        }
        if (ownerConnection == null) {
            throw new BadRequestException("Chủ sở hữu Form chưa kết nối tài khoản Google hợp lệ.");
        }

        integration.setSyncStatus("SYNCING");
        integrationRepo.save(integration);

        try {
            Forms formsClient = formsService.getFormsClient(ownerConnection);
            String formId = integration.getGoogleFormId();

            List<FormResponse> responsesList = new ArrayList<>();
            String pageToken = null;

            do {
                var listReq = formsClient.forms().responses().list(formId);
                if (pageToken != null) {
                    listReq.setPageToken(pageToken);
                }
                ListFormResponsesResponse listResp = listReq.execute();
                if (listResp.getResponses() != null) {
                    responsesList.addAll(listResp.getResponses());
                }
                pageToken = listResp.getNextPageToken();
            } while (pageToken != null && !pageToken.isBlank());

            // Build question mappings
            List<GoogleQuestionMapping> mappings = mappingRepo.findBySurveyId(surveyId);
            Map<String, GoogleQuestionMapping> questionMap = new HashMap<>();
            for (GoogleQuestionMapping m : mappings) {
                questionMap.put(m.getGoogleQuestionId(), m);
            }

            int syncedCount = 0;

            for (FormResponse gResp : responsesList) {
                String googleRespId = gResp.getResponseId();
                if (externalResponseRepo.findByGoogleFormIdAndGoogleResponseId(formId, googleRespId).isPresent()) {
                    continue; // Skip already synced
                }

                Instant submittedAt = Instant.now();
                if (gResp.getCreateTime() != null) {
                    try {
                        submittedAt = Instant.parse(gResp.getCreateTime());
                    } catch (Exception ignored) {}
                }

                String answersJson = gsonFactory.toString(gResp);

                // 1. Save ExternalSurveyResponse
                ExternalSurveyResponse externalResp = new ExternalSurveyResponse();
                externalResp.setSurveyId(surveyId);
                externalResp.setGoogleFormId(formId);
                externalResp.setGoogleResponseId(googleRespId);
                externalResp.setSubmittedAt(submittedAt);
                externalResp.setAnswersJson(answersJson != null ? answersJson : "{}");
                externalResp.setSyncStatus("SYNCED");
                externalResp.setSyncedAt(Instant.now());
                externalResp = externalResponseRepo.save(externalResp);

                // 2. Normalize into internal Response and Answers if mapping exists
                if (!questionMap.isEmpty() && gResp.getAnswers() != null) {
                    Response internalResp = new Response();
                    internalResp.setSurvey(survey);
                    internalResp.setRequestId(UUID.randomUUID());
                    internalResp.setToken("GOOGLE_FORMS_" + googleRespId);
                    internalResp.setSubmittedAtInstant(submittedAt);
                    internalResp = responseRepo.save(internalResp);

                    int answeredCount = 0;

                    for (Map.Entry<String, com.google.api.services.forms.v1.model.Answer> entry : gResp.getAnswers().entrySet()) {
                        String googleQId = entry.getKey();
                        com.google.api.services.forms.v1.model.Answer gAnswer = entry.getValue();

                        GoogleQuestionMapping mapping = questionMap.get(googleQId);
                        if (mapping == null) continue;

                        Question question = questionRepo.findById(mapping.getInternalQuestionId()).orElse(null);
                        if (question == null) continue;

                        if (gAnswer.getTextAnswers() == null || gAnswer.getTextAnswers().getAnswers() == null) continue;
                        List<com.google.api.services.forms.v1.model.TextAnswer> textAnswers = gAnswer.getTextAnswers().getAnswers();
                        if (textAnswers.isEmpty()) continue;

                        Answer internalAnswer = new Answer();
                        internalAnswer.setResponse(internalResp);
                        internalAnswer.setQuestion(question);

                        String firstVal = textAnswers.get(0).getValue();

                        if ("SINGLE_CHOICE".equals(mapping.getQuestionType()) || "MULTIPLE_CHOICE".equals(mapping.getQuestionType())) {
                            List<Option> options = optionRepo.findByQuestion_IdOrderByOrderIndexAsc(question.getId());
                            List<AnswerOption> chosenOptions = new ArrayList<>();

                            for (com.google.api.services.forms.v1.model.TextAnswer ta : textAnswers) {
                                String val = ta.getValue();
                                for (Option opt : options) {
                                    if (opt.getOptionText() != null && opt.getOptionText().equalsIgnoreCase(val)) {
                                        AnswerOption ao = new AnswerOption();
                                        ao.setAnswer(internalAnswer);
                                        ao.setOption(opt);
                                        chosenOptions.add(ao);
                                        break;
                                    }
                                }
                            }
                            internalAnswer.setAnswerOptions(chosenOptions);
                            internalAnswer.setAnswerText(firstVal);
                        } else if ("NUMBER".equals(mapping.getQuestionType())) {
                            internalAnswer.setAnswerText(firstVal);
                            try {
                                internalAnswer.setAnswerNumber(new BigDecimal(firstVal.trim()));
                            } catch (Exception ignored) {}
                        } else if ("DATE".equals(mapping.getQuestionType())) {
                            internalAnswer.setAnswerText(firstVal);
                            try {
                                internalAnswer.setAnswerDate(LocalDateTime.parse(firstVal.trim()));
                            } catch (Exception ignored) {}
                        } else {
                            internalAnswer.setAnswerText(firstVal);
                        }

                        answerRepo.save(internalAnswer);
                        answeredCount++;
                    }

                    internalResp.setAnsweredQuestionCount(answeredCount);
                    internalResp.setApplicableQuestionCount(mappings.size());
                    responseRepo.save(internalResp);

                    externalResp.setInternalResponseId(internalResp.getId());
                    externalResponseRepo.save(externalResp);
                }

                syncedCount++;
            }

            // Check and sync Google Form open/close state
            boolean isAccepting = formsService.isFormAcceptingResponses(ownerConnection, formId, integration.getResponseUrl());
            survey.setManuallyLocked(!isAccepting);
            survey.setRequiresUnlock(false);
            survey.setOpenAt(null);
            survey.setCloseAt(null);
            surveyRepo.save(survey);

            integration.setSyncStatus("SUCCESS");
            integration.setLastSyncedAt(Instant.now());
            integration.setLastSyncError(null);
            integrationRepo.save(integration);

            long total = externalResponseRepo.countBySurveyId(surveyId);
            String statusMsg = isAccepting ? "Đang mở nhận phản hồi" : "Đã đóng ngừng nhận phản hồi";
            return new SyncResult(
                    surveyId,
                    syncedCount,
                    (int) total,
                    "SUCCESS",
                    "Đồng bộ thành công " + syncedCount + " phản hồi mới. Trạng thái Google Forms: " + statusMsg + ".",
                    isAccepting
            );
        } catch (Exception e) {
            log.error("Sync failed for survey {}: {}", surveyId, e.getMessage(), e);
            integration.setSyncStatus("FAILED");
            integration.setLastSyncError(e.getMessage());
            integrationRepo.save(integration);
            throw new BadRequestException("Đồng bộ câu trả lời thất bại: " + e.getMessage());
        }
    }
}
