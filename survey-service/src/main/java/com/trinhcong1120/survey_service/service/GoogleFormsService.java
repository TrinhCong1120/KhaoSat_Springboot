package com.trinhcong1120.survey_service.service;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.model.BatchUpdateFormRequest;
import com.google.api.services.forms.v1.model.Form;
import com.google.api.services.forms.v1.model.Info;
import com.google.api.services.forms.v1.model.Item;
import com.google.api.services.forms.v1.model.QuestionItem;
import com.google.api.services.forms.v1.model.Request;
import com.google.api.services.forms.v1.model.BatchUpdateFormResponse;
import com.google.api.services.forms.v1.model.ChoiceQuestion;
import com.google.api.services.forms.v1.model.CreateItemRequest;
import com.google.api.services.forms.v1.model.CreateItemResponse;
import com.google.api.services.forms.v1.model.DateQuestion;
import com.google.api.services.forms.v1.model.DeleteItemRequest;
import com.google.api.services.forms.v1.model.Location;
import com.google.api.services.forms.v1.model.TextQuestion;
import com.google.api.services.forms.v1.model.UpdateFormInfoRequest;
import com.google.api.services.forms.v1.model.UpdateItemRequest;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class GoogleFormsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleFormsService.class);

    private final GoogleOAuthService oAuthService;
    private final PageRepository pageRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;
    private final QuestionTypeRepository questionTypeRepo;
    private final GoogleQuestionMappingRepository mappingRepo;
    private final SurveyIntegrationRepository integrationRepo;
    private final GoogleConnectionRepository connectionRepo;

    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    public GoogleFormsService(
            GoogleOAuthService oAuthService,
            PageRepository pageRepo,
            QuestionRepository questionRepo,
            OptionRepository optionRepo,
            QuestionTypeRepository questionTypeRepo,
            GoogleQuestionMappingRepository mappingRepo,
            SurveyIntegrationRepository integrationRepo,
            GoogleConnectionRepository connectionRepo
    ) {
        this.oAuthService = oAuthService;
        this.pageRepo = pageRepo;
        this.questionRepo = questionRepo;
        this.optionRepo = optionRepo;
        this.questionTypeRepo = questionTypeRepo;
        this.mappingRepo = mappingRepo;
        this.integrationRepo = integrationRepo;
        this.connectionRepo = connectionRepo;
    }

    public Forms getFormsClient(GoogleConnection connection) {
        String accessToken = oAuthService.getValidAccessToken(connection);
        HttpRequestInitializer initializer = request -> request.getHeaders().setAuthorization("Bearer " + accessToken);
        return new Forms.Builder(httpTransport, jsonFactory, initializer)
                .setApplicationName("SurveyApp")
                .build();
    }

    public Form createForm(GoogleConnection connection, String title, String description) {
        try {
            Forms formsClient = getFormsClient(connection);

            Form form = new Form();
            Info info = new Info();
            info.setTitle(title);
            info.setDocumentTitle(title);
            form.setInfo(info);

            Form createdForm = formsClient.forms().create(form).execute();

            if (description != null && !description.isBlank()) {
                UpdateFormInfoRequest updateInfo = new UpdateFormInfoRequest();
                Info newInfo = new Info();
                newInfo.setDescription(description);
                updateInfo.setInfo(newInfo);
                updateInfo.setUpdateMask("description");

                Request request = new Request();
                request.setUpdateFormInfo(updateInfo);

                BatchUpdateFormRequest batch = new BatchUpdateFormRequest();
                batch.setRequests(List.of(request));
                formsClient.forms().batchUpdate(createdForm.getFormId(), batch).execute();
            }

            return createdForm;
        } catch (Exception e) {
            log.error("Failed to create Google Form: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tạo Google Form: " + e.getMessage());
        }
    }

    public Form getForm(GoogleConnection connection, String formId) {
        try {
            Forms formsClient = getFormsClient(connection);
            return formsClient.forms().get(formId).execute();
        } catch (Exception e) {
            log.error("Failed to get Google Form {}: {}", formId, e.getMessage());
            throw new BadRequestException("Không thể truy cập Google Form (kiểm tra lại quyền truy cập hoặc ID form): " + e.getMessage());
        }
    }

    public boolean isFormAcceptingResponses(GoogleConnection connection, String formId, String responseUrl) {
        // 1. Try Google Forms API publishSettings
        try {
            Form form = getForm(connection, formId);
            Object pubSettings = form.get("publishSettings");
            if (pubSettings instanceof Map<?, ?> pubMap) {
                Object pubState = pubMap.get("publishState");
                if (pubState instanceof Map<?, ?> stateMap) {
                    Object isAccepting = stateMap.get("isAcceptingResponses");
                    if (isAccepting instanceof Boolean b) {
                        log.info("Google Form {} isAcceptingResponses from publishSettings: {}", formId, b);
                        return b;
                    }
                    Object isPublished = stateMap.get("isPublished");
                    if (Boolean.FALSE.equals(isPublished)) {
                        log.info("Google Form {} isPublished is false", formId);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not check publishSettings via Forms API for {}: {}", formId, e.getMessage());
        }

        // 2. Fallback: Probe public responder URL HTML
        String urlToCheck = responseUrl;
        if (urlToCheck == null || urlToCheck.isBlank()) {
            urlToCheck = "https://docs.google.com/forms/d/e/" + formId + "/viewform";
        }
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(urlToCheck))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(java.time.Duration.ofSeconds(6))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body != null) {
                if (body.contains("freebirdFormviewerViewResponseFormClosed")
                        || body.contains("no longer accepting responses")
                        || body.contains("không còn nhận câu trả lời")
                        || body.contains("không còn chấp nhận câu trả lời")) {
                    log.info("Google Form {} responderUrl indicated form is closed", formId);
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("Could not probe responder URL for {}: {}", formId, e.getMessage());
        }

        return true;
    }

    public void pullFormSchema(Survey survey, GoogleConnection connection, String googleFormId) {
        Form form = getForm(connection, googleFormId);

        // Clear existing mappings for this survey
        mappingRepo.deleteBySurveyId(survey.getId());

        // Find or create default page
        List<Page> existingPages = pageRepo.findBySurvey_IdOrderByOrderIndexAsc(survey.getId());
        Page page;
        if (existingPages.isEmpty()) {
            page = new Page();
            page.setSurvey(survey);
            page.setTitle(survey.getTitle() != null ? survey.getTitle() : "Trang 1");
            page.setOrderIndex(1);
            page = pageRepo.save(page);
        } else {
            page = existingPages.get(0);
        }

        // Clean existing questions of this page to avoid duplicates when pulling schema
        List<Question> oldQuestions = questionRepo.findByPage_IdOrderByOrderIndexAsc(page.getId());
        for (Question q : oldQuestions) {
            optionRepo.deleteByQuestion_Id(q.getId());
            questionRepo.delete(q);
        }

        if (form.getItems() == null || form.getItems().isEmpty()) {
            return;
        }

        Map<String, QuestionType> typeCache = new HashMap<>();

        int orderIdx = 1;
        for (Item item : form.getItems()) {
            if (item.getQuestionItem() != null) {
                QuestionItem qItem = item.getQuestionItem();
                com.google.api.services.forms.v1.model.Question gQuestion = qItem.getQuestion();
                if (gQuestion == null) continue;

                String typeCode = resolveQuestionTypeCode(gQuestion);
                QuestionType qType = typeCache.computeIfAbsent(typeCode, code ->
                        questionTypeRepo.findByCode(code).orElseGet(() -> questionTypeRepo.findByCode("TEXT").orElse(null))
                );

                Question question = new Question();
                question.setPage(page);
                question.setQuestionText(item.getTitle() != null ? item.getTitle() : "Câu hỏi " + orderIdx);
                question.setDescription(item.getDescription());
                question.setIsRequired(Boolean.TRUE.equals(gQuestion.getRequired()));
                question.setOrderIndex(orderIdx++);
                question.setQuestionType(qType);
                question = questionRepo.save(question);

                // If choice question, create options
                if (gQuestion.getChoiceQuestion() != null && gQuestion.getChoiceQuestion().getOptions() != null) {
                    int optIdx = 1;
                    for (com.google.api.services.forms.v1.model.Option gOpt : gQuestion.getChoiceQuestion().getOptions()) {
                        Option opt = new Option();
                        opt.setQuestion(question);
                        opt.setOptionText(gOpt.getValue() != null ? gOpt.getValue() : "Lựa chọn " + optIdx);
                        opt.setOrderIndex(optIdx++);
                        optionRepo.save(opt);
                    }
                }

                // Save mapping
                GoogleQuestionMapping mapping = new GoogleQuestionMapping();
                mapping.setSurveyId(survey.getId());
                mapping.setInternalQuestionId(question.getId());
                mapping.setGoogleFormId(googleFormId);
                mapping.setGoogleItemId(item.getItemId() != null ? item.getItemId() : "");
                mapping.setGoogleQuestionId(gQuestion.getQuestionId() != null ? gQuestion.getQuestionId() : item.getItemId());
                mapping.setQuestionType(typeCode);
                mapping.setUpdatedAt(Instant.now());
                mappingRepo.save(mapping);
            }
        }
    }

    private String resolveQuestionTypeCode(com.google.api.services.forms.v1.model.Question gQuestion) {
        if (gQuestion.getChoiceQuestion() != null) {
            String type = gQuestion.getChoiceQuestion().getType();
            if ("CHECKBOX".equalsIgnoreCase(type)) {
                return "MULTIPLE_CHOICE";
            } else {
                return "SINGLE_CHOICE";
            }
        }
        if (gQuestion.getDateQuestion() != null) {
            return "DATE";
        }
        if (gQuestion.getScaleQuestion() != null) {
            return "NUMBER";
        }
        return "TEXT";
    }

    private GoogleConnection getOwnerConnection(SurveyIntegration integ) {
        if (integ == null || integ.getOwnerConnectionId() == null) return null;
        return connectionRepo.findById(integ.getOwnerConnectionId()).orElse(null);
    }

    public void syncQuestionCreated(Survey survey, Question question, List<Option> options) {
        try {
            Optional<SurveyIntegration> integOpt = integrationRepo.findBySurveyId(survey.getId());
            if (integOpt.isEmpty()) return;
            SurveyIntegration integ = integOpt.get();
            GoogleConnection conn = getOwnerConnection(integ);
            if (conn == null) return;

            Forms formsClient = getFormsClient(conn);
            Form currentForm = formsClient.forms().get(integ.getGoogleFormId()).execute();
            int index = currentForm.getItems() != null ? currentForm.getItems().size() : 0;

            Item item = buildGoogleItem(question, options);

            CreateItemRequest createItem = new CreateItemRequest();
            createItem.setItem(item);
            Location location = new Location();
            location.setIndex(index);
            createItem.setLocation(location);

            Request req = new Request();
            req.setCreateItem(createItem);

            BatchUpdateFormRequest batch = new BatchUpdateFormRequest();
            batch.setRequests(List.of(req));

            BatchUpdateFormResponse batchRes = formsClient.forms().batchUpdate(integ.getGoogleFormId(), batch).execute();
            if (batchRes.getReplies() != null && !batchRes.getReplies().isEmpty()) {
                CreateItemResponse createRes = batchRes.getReplies().get(0).getCreateItem();
                String itemId = createRes.getItemId();
                List<String> questionIds = createRes.getQuestionId();
                String questionId = (questionIds != null && !questionIds.isEmpty()) ? questionIds.get(0) : itemId;

                GoogleQuestionMapping mapping = new GoogleQuestionMapping();
                mapping.setSurveyId(survey.getId());
                mapping.setInternalQuestionId(question.getId());
                mapping.setGoogleFormId(integ.getGoogleFormId());
                mapping.setGoogleItemId(itemId != null ? itemId : "");
                mapping.setGoogleQuestionId(questionId != null ? questionId : itemId);
                mapping.setQuestionType(question.getQuestionType() != null ? question.getQuestionType().getCode() : "TEXT");
                mapping.setUpdatedAt(Instant.now());
                mappingRepo.save(mapping);
                log.info("Synced newly created question '{}' to Google Form {}", question.getQuestionText(), integ.getGoogleFormId());
            }
        } catch (Exception e) {
            log.error("Failed to sync question creation to Google Form: {}", e.getMessage(), e);
        }
    }

    public void syncQuestionUpdated(Survey survey, Question question, List<Option> options) {
        try {
            Optional<SurveyIntegration> integOpt = integrationRepo.findBySurveyId(survey.getId());
            if (integOpt.isEmpty()) return;
            SurveyIntegration integ = integOpt.get();
            GoogleConnection conn = getOwnerConnection(integ);
            if (conn == null) return;

            Optional<GoogleQuestionMapping> mappingOpt = mappingRepo.findBySurveyIdAndInternalQuestionId(survey.getId(), question.getId());
            if (mappingOpt.isEmpty()) {
                syncQuestionCreated(survey, question, options);
                return;
            }
            GoogleQuestionMapping mapping = mappingOpt.get();

            Forms formsClient = getFormsClient(conn);
            Form currentForm = formsClient.forms().get(integ.getGoogleFormId()).execute();
            if (currentForm.getItems() == null) return;

            int foundIndex = -1;
            for (int i = 0; i < currentForm.getItems().size(); i++) {
                Item item = currentForm.getItems().get(i);
                if (mapping.getGoogleItemId() != null && mapping.getGoogleItemId().equals(item.getItemId())) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex < 0) {
                syncQuestionCreated(survey, question, options);
                return;
            }

            Item updatedItem = buildGoogleItem(question, options);
            updatedItem.setItemId(mapping.getGoogleItemId());

            UpdateItemRequest updateReq = new UpdateItemRequest();
            updateReq.setItem(updatedItem);
            Location location = new Location();
            location.setIndex(foundIndex);
            updateReq.setLocation(location);
            updateReq.setUpdateMask("title,description,questionItem");

            Request req = new Request();
            req.setUpdateItem(updateReq);

            BatchUpdateFormRequest batch = new BatchUpdateFormRequest();
            batch.setRequests(List.of(req));

            formsClient.forms().batchUpdate(integ.getGoogleFormId(), batch).execute();
            mapping.setQuestionType(question.getQuestionType() != null ? question.getQuestionType().getCode() : "TEXT");
            mapping.setUpdatedAt(Instant.now());
            mappingRepo.save(mapping);
            log.info("Synced updated question '{}' to Google Form {}", question.getQuestionText(), integ.getGoogleFormId());
        } catch (Exception e) {
            log.error("Failed to sync question update to Google Form: {}", e.getMessage(), e);
        }
    }

    public void syncQuestionDeleted(Survey survey, UUID questionId) {
        try {
            Optional<SurveyIntegration> integOpt = integrationRepo.findBySurveyId(survey.getId());
            if (integOpt.isEmpty()) return;
            SurveyIntegration integ = integOpt.get();
            GoogleConnection conn = getOwnerConnection(integ);
            if (conn == null) return;

            Optional<GoogleQuestionMapping> mappingOpt = mappingRepo.findBySurveyIdAndInternalQuestionId(survey.getId(), questionId);
            if (mappingOpt.isEmpty()) return;
            GoogleQuestionMapping mapping = mappingOpt.get();

            Forms formsClient = getFormsClient(conn);
            Form currentForm = formsClient.forms().get(integ.getGoogleFormId()).execute();
            if (currentForm.getItems() != null) {
                int foundIndex = -1;
                for (int i = 0; i < currentForm.getItems().size(); i++) {
                    Item item = currentForm.getItems().get(i);
                    if (mapping.getGoogleItemId() != null && mapping.getGoogleItemId().equals(item.getItemId())) {
                        foundIndex = i;
                        break;
                    }
                }

                if (foundIndex >= 0) {
                    DeleteItemRequest deleteReq = new DeleteItemRequest();
                    Location location = new Location();
                    location.setIndex(foundIndex);
                    deleteReq.setLocation(location);

                    Request req = new Request();
                    req.setDeleteItem(deleteReq);

                    BatchUpdateFormRequest batch = new BatchUpdateFormRequest();
                    batch.setRequests(List.of(req));

                    formsClient.forms().batchUpdate(integ.getGoogleFormId(), batch).execute();
                    log.info("Synced question deletion to Google Form {}", integ.getGoogleFormId());
                }
            }

            mappingRepo.deleteBySurveyIdAndInternalQuestionId(survey.getId(), questionId);
        } catch (Exception e) {
            log.error("Failed to sync question deletion to Google Form: {}", e.getMessage(), e);
        }
    }

    private Item buildGoogleItem(Question question, List<Option> options) {
        Item item = new Item();
        item.setTitle(question.getQuestionText() != null ? question.getQuestionText() : "Câu hỏi");
        if (question.getDescription() != null && !question.getDescription().isBlank()) {
            item.setDescription(question.getDescription());
        }

        QuestionItem qItem = new QuestionItem();
        com.google.api.services.forms.v1.model.Question gq = new com.google.api.services.forms.v1.model.Question();
        gq.setRequired(Boolean.TRUE.equals(question.getIsRequired()));

        String typeCode = question.getQuestionType() != null ? question.getQuestionType().getCode() : "TEXT";

        if ("SINGLE_CHOICE".equalsIgnoreCase(typeCode) || "MULTIPLE_CHOICE".equalsIgnoreCase(typeCode)) {
            ChoiceQuestion choice = new ChoiceQuestion();
            choice.setType("MULTIPLE_CHOICE".equalsIgnoreCase(typeCode) ? "CHECKBOX" : "RADIO");
            List<com.google.api.services.forms.v1.model.Option> gOpts = new ArrayList<>();
            if (options != null && !options.isEmpty()) {
                for (Option opt : options) {
                    com.google.api.services.forms.v1.model.Option gOpt = new com.google.api.services.forms.v1.model.Option();
                    gOpt.setValue(opt.getOptionText() != null ? opt.getOptionText() : "");
                    gOpts.add(gOpt);
                }
            } else {
                com.google.api.services.forms.v1.model.Option defaultOpt = new com.google.api.services.forms.v1.model.Option();
                defaultOpt.setValue("Tùy chọn 1");
                gOpts.add(defaultOpt);
            }
            choice.setOptions(gOpts);
            gq.setChoiceQuestion(choice);
        } else if ("DATE".equalsIgnoreCase(typeCode)) {
            DateQuestion dq = new DateQuestion();
            dq.setIncludeYear(true);
            dq.setIncludeTime(false);
            gq.setDateQuestion(dq);
        } else {
            TextQuestion tq = new TextQuestion();
            tq.setParagraph("TEXTAREA".equalsIgnoreCase(typeCode));
            gq.setTextQuestion(tq);
        }

        qItem.setQuestion(gq);
        item.setQuestionItem(qItem);
        return item;
    }
}
