package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.condition.*;
import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class ConditionService {

  private final ConditionRepository conditionRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final SurveyRepository surveyRepository;

  public ConditionService(
          ConditionRepository conditionRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          SurveyRepository surveyRepository
  ) {
    this.conditionRepository = conditionRepository;
    this.guard = guard;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.surveyRepository = surveyRepository;
  }

  @Transactional(readOnly = true)
  public List<ConditionResponse> getAll() {
    List<UUID> surveyIds = guard.visibleSurveys().stream().map(Survey::getId).toList();
    if (surveyIds.isEmpty()) return List.of();
    return conditionRepository.findBySurveyIds(surveyIds)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<ConditionResponse> getBySurvey(
          UUID surveyId
  ) {
    guard.view(surveyId);
    return conditionRepository
            .findSafeBySurveyId(surveyId)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  public ConditionResponse create(
          CreateConditionRequest request
  ) {
    guard.editConditionQuestions(request.getSourceQuestionId(), request.getTargetQuestionId());
    Question source = getQuestion(
            request.getSourceQuestionId());

    Question target = getQuestion(
            request.getTargetQuestionId());

    String action = validateAction(request.getAction());

    String value =
            normalizeSourceValue(
                    source,
                    request.getSourceValue()
            );

    Condition condition = new Condition();

    condition.setSourceQuestion(source);
    condition.setSourceValue(value);
    condition.setTargetQuestion(target);
    condition.setAction(action);

    Condition saved = conditionRepository.save(condition);
    bumpRevision(target);

    return toResponse(saved);
  }

  public ConditionResponse update(
          UUID id,
          UpdateConditionRequest request
  ) {
    guard.editCondition(id);
    guard.editConditionQuestions(request.getSourceQuestionId(), request.getTargetQuestionId());
    Condition condition =
            conditionRepository.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Condition không tồn tại"));

    UUID originalSurveyId = condition.getSourceQuestion().getPage().getSurvey().getId();
    if (!originalSurveyId.equals(guard.questionSurvey(request.getSourceQuestionId()))) {
      throw new BadRequestException("Khong duoc chuyen condition sang survey khac");
    }

    Question source =
            getQuestion(request.getSourceQuestionId());

    Question target =
            getQuestion(request.getTargetQuestionId());

    condition.setSourceQuestion(source);
    condition.setTargetQuestion(target);
    condition.setAction(
            validateAction(request.getAction())
    );

    condition.setSourceValue(
            normalizeSourceValue(
                    source,
                    request.getSourceValue()
            )
    );

    Condition saved = conditionRepository.save(condition);
    bumpRevision(target);

    return toResponse(saved);
  }

  public void delete(UUID id) {
    guard.editCondition(id);

    Condition condition =
            conditionRepository.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Condition không tồn tại"));

    conditionRepository.delete(condition);
    bumpRevision(condition.getTargetQuestion());
  }

  private void bumpRevision(Question question) {
    if (question == null
            || question.getPage() == null
            || question.getPage().getSurvey() == null) {
      return;
    }

    Survey survey = question.getPage().getSurvey();
    survey.incrementValidationRevision();
    surveyRepository.save(survey);
  }

  private Question getQuestion(UUID id) {
    return questionRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Question không tồn tại"));
  }

  private String validateAction(String action) {

    if (action == null) {
      throw new BadRequestException(
              "Action không hợp lệ");
    }

    String result = action.trim().toUpperCase();

    if (!result.equals("SHOW")
            && !result.equals("HIDE")) {
      throw new BadRequestException(
              "Action phải là SHOW hoặc HIDE");
    }

    return result;
  }

  private String normalizeSourceValue(
          Question source,
          String value
  ) {
    if (value == null || value.isBlank()) {
      throw new BadRequestException(
              "Source value không được để trống");
    }

    String code =
            source.getQuestionType() == null
                    ? ""
                    : source.getQuestionType().getCode();

    if ("ADDRESS".equalsIgnoreCase(code)) {

      Map<String, String> values =
              new LinkedHashMap<>();

      for (String item : value.split(",")) {

        String text = item.trim();

        if (!text.isBlank()) {
          values.putIfAbsent(
                  text.toLowerCase(),
                  text
          );
        }
      }

      return String.join(
              ",",
              values.values()
      );
    }

    if ("SINGLE_CHOICE".equalsIgnoreCase(code)
            || "MULTIPLE_CHOICE".equalsIgnoreCase(code)) {

      LinkedHashSet<UUID> ids =
              new LinkedHashSet<>();

      for (String item : value.split(",")) {

        try {

          UUID optionId = UUID.fromString(item.trim());

          Option option =
                  optionRepository
                          .findById(optionId)
                          .orElseThrow(() ->
                                  new BadRequestException(
                                          "Option không tồn tại"));

          if (!option.getQuestion()
                  .getId()
                  .equals(source.getId())) {

            throw new BadRequestException(
                    "Option không thuộc source question");
          }

          ids.add(optionId);

        } catch (IllegalArgumentException e) {

          throw new BadRequestException(
                  "Source value phải chứa option ID hợp lệ");
        }
      }

      return ids.stream()
              .map(String::valueOf)
              .reduce(
                      (a, b) -> a + "," + b
              )
              .orElse("");
    }

    return value.trim();
  }

  private ConditionResponse toResponse(
          Condition condition
  ) {
    return new ConditionResponse(
            condition.getId(),
            condition.getSourceQuestion().getId(),
            condition.getSourceValue(),
            condition.getTargetQuestion().getId(),
            condition.getAction()
    );
  }
}
