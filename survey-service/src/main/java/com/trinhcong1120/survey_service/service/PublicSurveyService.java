package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.OptionResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;
import com.trinhcong1120.survey_service.dto.submit.*;
import com.trinhcong1120.survey_service.dto.survey.SurveyDetailResponse;
import com.trinhcong1120.survey_service.dto.validation.ValidateSurveyRequest;
import com.trinhcong1120.survey_service.dto.validation.ValidateSurveyResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.exception.SurveyApiException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class PublicSurveyService {

  private final SurveyRepository surveyRepository;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final ConditionRepository conditionRepository;
  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final MediaFileRepository mediaFileRepository;
  private final AnswerValidationService answerValidationService;
  private final QuestionValidationRuleService questionValidationRuleService;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;
  private final SurveyScheduleService schedule;
  private final SurveyTrackingService tracking;

  public PublicSurveyService(
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ConditionRepository conditionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          MediaFileRepository mediaFileRepository,
          AnswerValidationService answerValidationService,
          QuestionValidationRuleService questionValidationRuleService,
          ObjectMapper objectMapper,
          TransactionTemplate transactionTemplate,
          SurveyScheduleService schedule
  ) {
    this(surveyRepository, pageRepository, questionRepository, optionRepository,
        conditionRepository, responseRepository, answerRepository, answerOptionRepository,
        mediaFileRepository, answerValidationService, questionValidationRuleService,
        objectMapper, transactionTemplate, schedule, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public PublicSurveyService(
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ConditionRepository conditionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          MediaFileRepository mediaFileRepository,
          AnswerValidationService answerValidationService,
          QuestionValidationRuleService questionValidationRuleService,
          ObjectMapper objectMapper,
          TransactionTemplate transactionTemplate,
          SurveyScheduleService schedule,
          SurveyTrackingService tracking
  ) {
    this.surveyRepository = surveyRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.conditionRepository = conditionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.mediaFileRepository = mediaFileRepository;
    this.answerValidationService = answerValidationService;
    this.questionValidationRuleService = questionValidationRuleService;
    this.objectMapper = objectMapper;
    this.transactionTemplate = transactionTemplate;
    this.schedule = schedule;
    this.tracking = tracking;
  }

  @Transactional(readOnly = true)
  public UUID resolveSurveyId(String surveyRef) {
    try {
      return UUID.fromString(surveyRef);
    } catch (IllegalArgumentException ex) {
      return surveyRepository.findByShareCode(surveyRef)
              .map(Survey::getId)
              .orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
    }
  }

  @Transactional
  public SurveyDetailResponse getActiveSurvey(UUID surveyId) {
    Survey survey = surveyRepository.findForRead(surveyId)
            .orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
    Instant at = schedule.now();
    List<Page> openPages = requireOpen(survey, at);
    Set<UUID> openPageIds = openPages.stream().map(Page::getId).collect(java.util.stream.Collectors.toSet());

    SurveyDetailResponse response = new SurveyDetailResponse();
    response.setId(survey.getId());
    response.setTitle(survey.getTitle());
    response.setShareCode(survey.getShareCode());
    response.setDescription(survey.getDescription());
    response.setCreatorUser(survey.getCreatorUser());
    response.setCreatedAt(survey.getCreatedAt());
    response.setStatus(SurveyScheduleService.Status.OPEN.name());
    response.setOpenAt(schedule.toLocal(survey.getOpenAt()));
    response.setCloseAt(schedule.toLocal(survey.getCloseAt()));
    response.setServerTime(schedule.toLocal(at));
    response.setValidationRevision(survey.getValidationRevision());
    response.setMediaFiles(toMediaResponses("SURVEY", survey.getId()));
    response.setPages(openPages.stream().map(this::toPageDetailResponse).toList());
    List<Condition> conditions = conditionRepository
            .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(surveyId, surveyId);
    Set<UUID> excluded = ConditionUtil.unreachableShowTargets(conditions, openPageIds);
    response.getPages().forEach(p -> p.setQuestions(p.getQuestions().stream()
            .filter(q -> !excluded.contains(q.getId())).toList()));
    response.setConditions(conditions.stream().filter(this::hasConditionQuestions)
            .filter(c -> openPageIds.contains(c.getSourceQuestion().getPage().getId())
                    && openPageIds.contains(c.getTargetQuestion().getPage().getId())
                    && !excluded.contains(c.getSourceQuestion().getId())
                    && !excluded.contains(c.getTargetQuestion().getId()))
            .map(this::toConditionResponse).toList());

    return response;
  }

  @Transactional(readOnly = true)
  public Survey getActiveSurveyEntity(UUID surveyId) {
    Survey survey = surveyRepository
            .findById(surveyId)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Survey khong ton tai"));
    requireOpen(survey, schedule.now());
    return survey;
  }

  private List<Page> requireOpen(Survey survey, Instant at) {
    List<Page> surveyPages = pageRepository.findBySurvey_IdOrderByOrderIndexAsc(survey.getId());
    SurveyScheduleService.Status status = schedule.status(survey, surveyPages, at);
    if (status == SurveyScheduleService.Status.DRAFT)
      throw new NotFoundException("Survey chua xuat ban");
    if (status == SurveyScheduleService.Status.SCHEDULED) {
      boolean phaseEnded = surveyPages.stream()
              .map(page -> schedule.effectiveClose(survey, page))
              .anyMatch(close -> close != null && !at.isBefore(close));
      throw new SurveyApiException("SURVEY_NOT_OPEN", HttpStatus.CONFLICT, phaseEnded
              ? "Khảo sát đã kết thúc một giai đoạn và đang chờ giai đoạn tiếp theo"
              : "Khao sat hien chua duoc mo");
    }
    if (status == SurveyScheduleService.Status.CLOSED)
      throw new SurveyApiException("SURVEY_CLOSED", HttpStatus.GONE, "Khao sat da dong");
    return schedule.openPages(survey, at);
  }

  public SubmitSurveyResponse submit(
          UUID surveyId,
          SubmitSurveyRequest request
  ) {
    UUID requestId = request.getRequestId() == null ? UUID.randomUUID() : request.getRequestId();
    String hash = requestHash(request);
    try {
      return transactionTemplate.execute(tx -> {
        Optional<Response> existing = responseRepository.findByRequestId(requestId);
        if (existing.isPresent()) return oldResponse(existing.get(), surveyId, hash);
        Survey survey = surveyRepository.findForRead(surveyId)
                .orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
        Instant at = schedule.now();
        Set<UUID> pageIds = requireOpen(survey, at).stream().map(Page::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (request.getSurveyRevision() == null || !Objects.equals(request.getSurveyRevision(), survey.getValidationRevision()))
          throw new SurveyApiException("SURVEY_REVISION_CONFLICT", HttpStatus.CONFLICT, "Du lieu khao sat da thay doi, vui long tai lai");
        List<SubmitAnswerRequest> answers = answerValidationService.validateAndFilter(surveyId, request.getAnswers(), pageIds);
        if (request.getSessionId() != null) tracking.verifySession(surveyId, request.getSessionId());
        int[] questionCounts = countQuestionCompleteness(surveyId, pageIds, answers);
        Response saved = doSaveResponse(survey, request, requestId, answers);
        saved.setApplicableQuestionCount(questionCounts[0]);
        saved.setAnsweredQuestionCount(questionCounts[1]);
        saved.setRequestHash(hash);
        responseRepository.saveAndFlush(saved);
        if (request.getSessionId() != null && tracking != null)
          tracking.recordActiveDuration(request.getSessionId(), request.getActiveDurationMs());
        return new SubmitSurveyResponse("Submit thanh cong", saved.getId(), requestId);
      });
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      return responseRepository.findByRequestId(requestId)
              .map(old -> oldResponse(old, surveyId, hash)).orElseThrow(() -> e);
    }
  }

  private SubmitSurveyResponse oldResponse(Response existing, UUID surveyId, String hash) {
    if (!Objects.equals(existing.getSurvey().getId(), surveyId)
            || existing.getRequestHash() != null && !existing.getRequestHash().equals(hash))
      throw new SurveyApiException("REQUEST_ID_CONFLICT", HttpStatus.CONFLICT, "requestId da duoc dung voi noi dung khac");
    return new SubmitSurveyResponse("Submit thanh cong", existing.getId(), existing.getRequestId());
  }

  private String requestHash(SubmitSurveyRequest request) {
    try {
      List<Object> hashParts = new ArrayList<>(List.of(
          request.getSurveyRevision() == null ? "" : request.getSurveyRevision(),
          request.getToken() == null ? "" : request.getToken(), request.getAnswers()));
      // Keep the old hash for legacy clients, but bind a new submission to its Session.
      if (request.getSessionId() != null) hashParts.add(request.getSessionId());
      byte[] bytes = objectMapper.writeValueAsBytes(hashParts);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (com.fasterxml.jackson.core.JsonProcessingException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Khong the tinh request hash", e);
    }
  }

  @Transactional
  public ValidateSurveyResponse validate(
          UUID surveyId,
          ValidateSurveyRequest request
  ) {
    Survey survey = surveyRepository.findForRead(surveyId)
            .orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
    Instant at = schedule.now();
    Set<UUID> openPageIds = requireOpen(survey, at).stream().map(Page::getId)
            .collect(java.util.stream.Collectors.toSet());

    if (request != null
            && request.getSurveyRevision() != null
            && !Objects.equals(
            request.getSurveyRevision(),
            survey.getValidationRevision()
    )) {
      throw new SurveyApiException("SURVEY_REVISION_CONFLICT", HttpStatus.CONFLICT, "Du lieu khao sat da thay doi, vui long tai lai");
    }

    AnswerValidationService.ValidationResult result =
            answerValidationService.validateSubmittedOnly(
                    surveyId,
                    request == null ? null : request.getAnswers(),
                    false,
                    openPageIds
            );

    ValidateSurveyResponse response = new ValidateSurveyResponse();
    response.setValid(result.valid());
    response.setSurveyRevision(survey.getValidationRevision());
    response.setEvaluatedAt(at);
    response.setFieldErrors(result.fieldErrors());
    response.setQuestionStates(
            result.applicability()
                    .entrySet()
                    .stream()
                    .map(entry ->
                            new ValidateSurveyResponse.QuestionState(
                                    entry.getKey(),
                                    entry.getValue(),
                                    entry.getValue()
                            )
                    )
                    .toList()
    );

    return response;
  }

  private Response doSaveResponse(
          Survey survey,
          SubmitSurveyRequest request,
          UUID requestId,
          List<SubmitAnswerRequest> answers
  ) {
    Response response = new Response();

    response.setSurvey(survey);
    response.setToken(request.getToken());
    response.setRequestId(requestId);
    response.setSubmittedAtInstant(java.time.Instant.now());
    response.setSessionId(request.getSessionId());

    response = responseRepository.save(response);

    if (answers == null) {
      return response;
    }

    for (SubmitAnswerRequest item :
            answers) {

      Question question =
              questionRepository
                      .findById(item.getQuestionId())
                      .orElseThrow(() ->
                              new NotFoundException(
                                      "Question khong ton tai"));

      if (!Objects.equals(
              question.getPage().getSurvey().getId(),
              survey.getId())) {
        throw new BadRequestException(
                "Question khong thuoc survey nay");
      }

      Answer answer = new Answer();

      answer.setResponse(response);
      answer.setQuestion(question);
      answer.setAnswerText(item.getAnswerText());
      answer.setAnswerNumber(item.getAnswerNumber());
      answer.setAnswerDate(item.getAnswerDate());
      answer.setProvinceCode(item.getProvinceCode());
      answer.setWardCode(item.getWardCode());
      answer.setProvince(item.getProvince());
      answer.setWard(item.getWard());
      answer.setAddressDetail(item.getAddressDetail());

      answer = answerRepository.save(answer);

      if (item.getOptionIds() == null) {
        continue;
      }

      for (UUID optionId :
              item.getOptionIds()) {

        Option option =
                optionRepository
                        .findById(optionId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Option khong ton tai"));

        if (!Objects.equals(
                option.getQuestion().getId(),
                question.getId())) {
          throw new BadRequestException(
                  "Option khong thuoc cau hoi nay");
        }

        AnswerOption answerOption =
                new AnswerOption();

        answerOption.setAnswer(answer);
        answerOption.setOption(option);

        answerOptionRepository.save(answerOption);
      }
    }

    return response;
  }

  private void validateRequiredAnswers(
          UUID surveyId,
          List<SubmitAnswerRequest> answers
  ) {
    List<Question> questions =
            questionRepository
                    .findByPage_Survey_Id(surveyId);

    List<Condition> conditions =
            conditionRepository
                    .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                            surveyId,
                            surveyId
                    );

    Map<UUID, Boolean> applicabilityMap =
            ConditionUtil.calculateApplicability(
                    questions,
                    toConditionAnswers(answers),
                    conditions
            );

    Map<UUID, SubmitAnswerRequest> answerMap =
            new HashMap<>();

    if (answers != null) {
      for (SubmitAnswerRequest answer : answers) {
        answerMap.put(
                answer.getQuestionId(),
                answer
        );
      }
    }

    for (Question question : questions) {

      if (!Boolean.TRUE.equals(
              question.getIsRequired())) {
        continue;
      }

      if (!applicabilityMap.getOrDefault(
              question.getId(),
              true
      )) {
        continue;
      }

      SubmitAnswerRequest answer =
              answerMap.get(question.getId());

      if (!hasValue(question, answer)) {
        throw new BadRequestException(
                "Cau hoi bat buoc chua duoc tra loi: "
                        + question.getQuestionText());
      }
    }
  }

  private int[] countQuestionCompleteness(UUID surveyId, Set<UUID> openPageIds,
      List<SubmitAnswerRequest> answers) {
    List<Question> questions = questionRepository.findByPage_Survey_Id(surveyId).stream()
        .filter(q -> openPageIds.contains(q.getPage().getId())).toList();
    List<Condition> conditions = conditionRepository
        .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(surveyId, surveyId);
    Map<UUID, Boolean> applicable = ConditionUtil.calculateApplicability(
        questions, toConditionAnswers(answers), conditions);
    Map<UUID, SubmitAnswerRequest> submitted = new HashMap<>();
    for (SubmitAnswerRequest answer : answers) submitted.put(answer.getQuestionId(), answer);
    int total = 0, answered = 0;
    for (Question question : questions) {
      if (!applicable.getOrDefault(question.getId(), true)) continue;
      total++;
      if (hasValue(question, submitted.get(question.getId()))) answered++;
    }
    return new int[]{total, answered};
  }

  private boolean hasValue(
          Question question,
          SubmitAnswerRequest answer
  ) {
    if (answer == null) {
      return false;
    }

    String type =
            getQuestionTypeCode(question);

    return switch (type) {

      case "SINGLE_CHOICE",
           "MULTIPLE_CHOICE" ->
              answer.getOptionIds() != null
                      && !answer.getOptionIds().isEmpty();

      case "NUMBER" ->
              answer.getAnswerNumber() != null;

      case "DATE" ->
              answer.getAnswerDate() != null;

      case "ADDRESS" ->
              answer.getProvince() != null
                      && !answer.getProvince().isBlank()
                      && answer.getWard() != null
                      && !answer.getWard().isBlank();

      default ->
              answer.getAnswerText() != null
                      && !answer.getAnswerText().isBlank();
    };
  }

  private void validateSubmittedAnswers(
          UUID surveyId,
          List<SubmitAnswerRequest> answers
  ) {
    if (answers == null) {
      return;
    }

    Set<UUID> questionIds =
            new HashSet<>();

    for (SubmitAnswerRequest answer : answers) {
      if (answer == null) {
        throw new BadRequestException(
                "Cau tra loi khong hop le");
      }

      if (answer.getQuestionId() == null) {
        throw new BadRequestException(
                "ID cau hoi khong duoc de trong");
      }

      if (!questionIds.add(answer.getQuestionId())) {
        throw new BadRequestException(
                "Cau hoi bi tra loi trung lap: "
                        + answer.getQuestionId());
      }

      Question question =
              questionRepository
                      .findById(answer.getQuestionId())
                      .orElseThrow(() ->
                              new NotFoundException(
                                      "Question khong ton tai"));

      if (!Objects.equals(
              question.getPage().getSurvey().getId(),
              surveyId)) {
        throw new BadRequestException(
                "Question khong thuoc survey nay");
      }

      validateAnswerType(
              question,
              answer
      );

      if (answer.getOptionIds() == null) {
        continue;
      }

      for (UUID optionId : answer.getOptionIds()) {
        if (optionId == null) {
          throw new BadRequestException(
                  "Option khong hop le");
        }

        if (!optionRepository.existsByIdAndQuestion_Id(
                optionId,
                question.getId()
        )) {
          throw new BadRequestException(
                  "Option khong thuoc cau hoi nay");
        }
      }
    }
  }

  private void validateAnswerType(
          Question question,
          SubmitAnswerRequest answer
  ) {
    String type =
            getQuestionTypeCode(question);

    switch (type) {
      case "SINGLE_CHOICE" ->
              validateChoiceAnswer(
                      question,
                      answer,
                      false
              );

      case "MULTIPLE_CHOICE" ->
              validateChoiceAnswer(
                      question,
                      answer,
                      true
              );

      case "NUMBER" ->
              validateNumberAnswer(
                      question,
                      answer
              );

      case "DATE" ->
              validateDateAnswer(
                      question,
                      answer
              );

      case "ADDRESS" ->
              validateAddressAnswer(
                      question,
                      answer
              );

      default ->
              validateTextAnswer(
                      question,
                      answer
              );
    }
  }

  private void validateChoiceAnswer(
          Question question,
          SubmitAnswerRequest answer,
          boolean multiple
  ) {
    if (hasText(answer)
            || answer.getAnswerNumber() != null
            || answer.getAnswerDate() != null
            || hasAddressValue(answer)) {
      throwInvalidAnswerType(question);
    }

    if (answer.getOptionIds() == null
            || answer.getOptionIds().isEmpty()) {
      return;
    }

    if (!multiple && answer.getOptionIds().size() > 1) {
      throw new BadRequestException(
              "Cau hoi chi duoc chon mot dap an: "
                      + question.getQuestionText());
    }

    Set<UUID> optionIds =
            new HashSet<>();

    for (UUID optionId : answer.getOptionIds()) {
      if (optionId == null
              || !optionIds.add(optionId)) {
        throw new BadRequestException(
                "Option khong hop le");
      }
    }
  }

  private void validateNumberAnswer(
          Question question,
          SubmitAnswerRequest answer
  ) {
    if (hasText(answer)
            || hasOptionIds(answer)
            || answer.getAnswerDate() != null
            || hasAddressValue(answer)) {
      throwInvalidAnswerType(question);
    }
  }

  private void validateDateAnswer(
          Question question,
          SubmitAnswerRequest answer
  ) {
    if (hasText(answer)
            || hasOptionIds(answer)
            || answer.getAnswerNumber() != null
            || hasAddressValue(answer)) {
      throwInvalidAnswerType(question);
    }
  }

  private void validateAddressAnswer(
          Question question,
          SubmitAnswerRequest answer
  ) {
    if (hasText(answer)
            || hasOptionIds(answer)
            || answer.getAnswerNumber() != null
            || answer.getAnswerDate() != null) {
      throwInvalidAnswerType(question);
    }

    if (!hasAddressValue(answer)) {
      return;
    }

    if (isBlank(answer.getProvince())
            || isBlank(answer.getWard())) {
      throw new BadRequestException(
              "Cau hoi dia chi phai co day du tinh/thanh va phuong/xa: "
                      + question.getQuestionText());
    }
  }

  private void validateTextAnswer(
          Question question,
          SubmitAnswerRequest answer
  ) {
    if (hasOptionIds(answer)
            || answer.getAnswerNumber() != null
            || answer.getAnswerDate() != null
            || hasAddressValue(answer)) {
      throwInvalidAnswerType(question);
    }
  }

  private void throwInvalidAnswerType(Question question) {
    throw new BadRequestException(
            "Kieu du lieu cau tra loi khong dung voi loai cau hoi: "
                    + question.getQuestionText());
  }

  private boolean hasText(SubmitAnswerRequest answer) {
    return !isBlank(answer.getAnswerText());
  }

  private boolean hasOptionIds(SubmitAnswerRequest answer) {
    return answer.getOptionIds() != null
            && !answer.getOptionIds().isEmpty();
  }

  private boolean hasAddressValue(SubmitAnswerRequest answer) {
    return answer.getProvinceCode() != null
            || answer.getWardCode() != null
            || !isBlank(answer.getProvince())
            || !isBlank(answer.getWard());
  }

  private boolean isBlank(String value) {
    return value == null
            || value.isBlank();
  }

  private String getQuestionTypeCode(Question question) {
    if (question == null
            || question.getQuestionType() == null
            || question.getQuestionType().getCode() == null) {
      return "";
    }

    return question.getQuestionType()
            .getCode()
            .trim()
            .toUpperCase();
  }

  private List<Answer> toConditionAnswers(
          List<SubmitAnswerRequest> requestAnswers
  ) {
    if (requestAnswers == null) {
      return List.of();
    }

    List<Answer> answers =
            new ArrayList<>();

    for (SubmitAnswerRequest requestAnswer : requestAnswers) {
      if (requestAnswer == null
              || requestAnswer.getQuestionId() == null) {
        continue;
      }

      Question question =
              questionRepository
                      .findById(requestAnswer.getQuestionId())
                      .orElse(null);

      if (question == null) {
        continue;
      }

      Answer answer = new Answer();
      answer.setQuestion(question);
      answer.setAnswerText(requestAnswer.getAnswerText());
      answer.setAnswerNumber(requestAnswer.getAnswerNumber());
      answer.setAnswerDate(requestAnswer.getAnswerDate());
      answer.setProvince(requestAnswer.getProvince());
      answer.setWard(requestAnswer.getWard());
      answer.setAddressDetail(requestAnswer.getAddressDetail());

      List<AnswerOption> answerOptions =
              new ArrayList<>();

      if (requestAnswer.getOptionIds() != null) {
        for (UUID optionId : requestAnswer.getOptionIds()) {
          optionRepository.findById(optionId)
                  .ifPresent(option -> {
                    AnswerOption answerOption =
                            new AnswerOption();

                    answerOption.setAnswer(answer);
                    answerOption.setOption(option);
                    answerOptions.add(answerOption);
                  });
        }
      }

      answer.setAnswerOptions(answerOptions);
      answers.add(answer);
    }

    return answers;
  }

  private List<SurveyDetailResponse.PageDetailResponse> toPageDetailResponses(
          UUID surveyId
  ) {
    return pageRepository.findBySurvey_IdOrderByOrderIndexAsc(surveyId)
            .stream()
            .map(this::toPageDetailResponse)
            .toList();
  }

  private SurveyDetailResponse.PageDetailResponse toPageDetailResponse(
          Page page
  ) {
    SurveyDetailResponse.PageDetailResponse response =
            new SurveyDetailResponse.PageDetailResponse(
            page.getId(),
            page.getTitle(),
            page.getDescription(),
            page.getOrderIndex(),
            toQuestionResponses(page.getId())
    );

    response.setMediaFiles(toMediaResponses("PAGE", page.getId()));
    response.setOpenAt(schedule.toLocal(page.getOpenAt()));
    response.setCloseAt(schedule.toLocal(page.getCloseAt()));
    response.setEffectiveCloseAt(schedule.toLocal(schedule.effectiveClose(page.getSurvey(), page)));
    return response;
  }

  private List<QuestionResponse> toQuestionResponses(UUID pageId) {
    return questionRepository.findByPage_IdOrderByOrderIndexAsc(pageId)
            .stream()
            .map(this::toQuestionResponse)
            .toList();
  }

  private QuestionResponse toQuestionResponse(Question question) {
    QuestionResponse response = new QuestionResponse(
            question.getId(),
            question.getPage().getId(),
            question.getQuestionText(),
            question.getQuestionType().getId(),
            question.getQuestionType().getCode(),
            question.getQuestionType().getName(),
            question.getIsRequired(),
            question.getOrderIndex(),
            question.getDescription(),
            toOptionResponses(question.getId())
    );

    response.setValidationRules(
            questionValidationRuleService.getByQuestion(question.getId())
    );
    response.setMediaFiles(toMediaResponses("QUESTION", question.getId()));

    return response;
  }

  private List<OptionResponse> toOptionResponses(UUID questionId) {
    return optionRepository.findByQuestion_IdOrderByOrderIndexAsc(questionId)
            .stream()
            .map(this::toOptionResponse)
            .toList();
  }

  private OptionResponse toOptionResponse(Option option) {
    OptionResponse response = new OptionResponse(
            option.getId(),
            option.getOptionText(),
            option.getOrderIndex()
    );

    response.setMediaFiles(toMediaResponses("OPTION", option.getId()));
    return response;
  }

  private List<ConditionResponse> toConditionResponses(UUID surveyId) {
    return conditionRepository
            .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                    surveyId,
                    surveyId
            )
            .stream()
            .filter(this::hasConditionQuestions)
            .map(this::toConditionResponse)
            .toList();
  }

  private boolean hasConditionQuestions(Condition condition) {
    return condition.getSourceQuestion() != null
            && condition.getTargetQuestion() != null;
  }

  private ConditionResponse toConditionResponse(Condition condition) {
    return new ConditionResponse(
            condition.getId(),
            condition.getSourceQuestion().getId(),
            condition.getSourceValue(),
            condition.getTargetQuestion().getId(),
            condition.getAction()
    );
  }

  private List<MediaUploadResponse> toMediaResponses(
          String ownerType,
          UUID ownerId
  ) {
    return mediaFileRepository
            .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(ownerType, ownerId)
            .stream()
            .map(MediaUploadResponse::fromEntity)
            .toList();
  }


}
