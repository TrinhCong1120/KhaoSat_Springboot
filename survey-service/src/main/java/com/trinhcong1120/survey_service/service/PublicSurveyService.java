package com.trinhcong1120.survey_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.question.OptionResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;
import com.trinhcong1120.survey_service.dto.submit.*;
import com.trinhcong1120.survey_service.dto.survey.SurveyDetailResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  public PublicSurveyService(
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ConditionRepository conditionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          ObjectMapper objectMapper,
          TransactionTemplate transactionTemplate
  ) {
    this.surveyRepository = surveyRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.conditionRepository = conditionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.objectMapper = objectMapper;
    this.transactionTemplate = transactionTemplate;
  }

  @Transactional(readOnly = true)
  public SurveyDetailResponse getActiveSurvey(Integer surveyId) {
    Survey survey = getActiveSurveyEntity(surveyId);

    SurveyDetailResponse response = new SurveyDetailResponse();
    response.setId(survey.getId());
    response.setTitle(survey.getTitle());
    response.setDescription(survey.getDescription());
    response.setCreatorUser(survey.getCreatorUser());
    response.setCreatedAt(survey.getCreatedAt());
    response.setIsActive(survey.getIsActive());
    response.setPages(toPageDetailResponses(surveyId));
    response.setConditions(toConditionResponses(surveyId));

    return response;
  }

  @Transactional(readOnly = true)
  public Survey getActiveSurveyEntity(Integer surveyId) {
    return surveyRepository
            .findByIdAndIsActiveTrue(surveyId)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Survey khong ton tai hoac chua duoc kich hoat"));
  }

  public SubmitSurveyResponse submit(
          Integer surveyId,
          SubmitSurveyRequest request
  ) {
    Survey survey = getActiveSurveyEntity(surveyId);

    validateSubmittedAnswers(
            surveyId,
            request.getAnswers()
    );

    validateRequiredAnswers(
            surveyId,
            request.getAnswers()
    );

    UUID requestId = UUID.randomUUID();

    try {
      Response response = saveResponse(
              survey,
              request,
              requestId
      );

      return new SubmitSurveyResponse(
              "Submit thanh cong",
              response.getId(),
              requestId
      );

    } catch (Exception e) {
      saveFailedSurvey(
              surveyId,
              request,
              requestId,
              e
      );

      return new SubmitSurveyResponse(
              "Submit thanh cong",
              0,
              requestId
      );
    }
  }

  private Response saveResponse(
          Survey survey,
          SubmitSurveyRequest request,
          UUID requestId
  ) {
    return transactionTemplate.execute(status ->
            doSaveResponse(
                    survey,
                    request,
                    requestId
            )
    );
  }

  private Response doSaveResponse(
          Survey survey,
          SubmitSurveyRequest request,
          UUID requestId
  ) {
    Response response = new Response();

    response.setSurvey(survey);
    response.setRequestId(requestId);
    response.setSubmittedAt(LocalDateTime.now());

    response = responseRepository.save(response);

    if (request.getAnswers() == null) {
      return response;
    }

    for (SubmitAnswerRequest item :
            request.getAnswers()) {

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

      answer = answerRepository.save(answer);

      if (item.getOptionIds() == null) {
        continue;
      }

      for (Integer optionId :
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

  private void saveFailedSurvey(
          Integer surveyId,
          SubmitSurveyRequest request,
          UUID requestId,
          Exception exception
  ) {
    try {
      Path directory =
              Paths.get(
                      "public",
                      "failed-surveys"
              ).toAbsolutePath().normalize();

      Files.createDirectories(directory);

      Path file =
              directory.resolve(
                      "failed_"
                              + LocalDateTime.now()
                              .format(
                                      DateTimeFormatter
                                              .ofPattern("yyyyMMdd")
                              )
                              + ".json"
              );

      List<Map<String, Object>> records =
              new ArrayList<>();

      if (Files.exists(file)) {
        records.addAll(
                objectMapper.readValue(
                        Files.readString(file),
                        objectMapper.getTypeFactory()
                                .constructCollectionType(
                                        List.class,
                                        Map.class
                                )
                )
        );
      }

      records.add(
              Map.of(
                      "requestId",
                      requestId,
                      "surveyId",
                      surveyId,
                      "failedAt",
                      LocalDateTime.now(),
                      "data",
                      request,
                      "error",
                      exception.getMessage() == null
                              ? exception.getClass().getSimpleName()
                              : exception.getMessage()
              )
      );

      objectMapper
              .writerWithDefaultPrettyPrinter()
              .writeValue(file.toFile(), records);

    } catch (Exception ignored) {
    }
  }

  private void validateRequiredAnswers(
          Integer surveyId,
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

    Map<Integer, Boolean> applicabilityMap =
            ConditionUtil.calculateApplicability(
                    questions,
                    toConditionAnswers(answers),
                    conditions
            );

    Map<Integer, SubmitAnswerRequest> answerMap =
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
          Integer surveyId,
          List<SubmitAnswerRequest> answers
  ) {
    if (answers == null) {
      return;
    }

    Set<Integer> questionIds =
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

      for (Integer optionId : answer.getOptionIds()) {
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

    Set<Integer> optionIds =
            new HashSet<>();

    for (Integer optionId : answer.getOptionIds()) {
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

      List<AnswerOption> answerOptions =
              new ArrayList<>();

      if (requestAnswer.getOptionIds() != null) {
        for (Integer optionId : requestAnswer.getOptionIds()) {
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
          Integer surveyId
  ) {
    return pageRepository.findBySurvey_IdOrderByOrderIndexAsc(surveyId)
            .stream()
            .map(this::toPageDetailResponse)
            .toList();
  }

  private SurveyDetailResponse.PageDetailResponse toPageDetailResponse(
          Page page
  ) {
    return new SurveyDetailResponse.PageDetailResponse(
            page.getId(),
            page.getTitle(),
            page.getOrderIndex(),
            toQuestionResponses(page.getId())
    );
  }

  private List<QuestionResponse> toQuestionResponses(Integer pageId) {
    return questionRepository.findByPage_IdOrderByOrderIndexAsc(pageId)
            .stream()
            .map(this::toQuestionResponse)
            .toList();
  }

  private QuestionResponse toQuestionResponse(Question question) {
    return new QuestionResponse(
            question.getId(),
            question.getPage().getId(),
            question.getQuestionText(),
            question.getQuestionType().getId(),
            question.getQuestionType().getCode(),
            question.getIsRequired(),
            question.getOrderIndex(),
            question.getDescription(),
            toOptionResponses(question.getId())
    );
  }

  private List<OptionResponse> toOptionResponses(Integer questionId) {
    return optionRepository.findByQuestion_IdOrderByOrderIndexAsc(questionId)
            .stream()
            .map(option ->
                    new OptionResponse(
                            option.getId(),
                            option.getOptionText(),
                            option.getOrderIndex()
                    )
            )
            .toList();
  }

  private List<ConditionResponse> toConditionResponses(Integer surveyId) {
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
}
