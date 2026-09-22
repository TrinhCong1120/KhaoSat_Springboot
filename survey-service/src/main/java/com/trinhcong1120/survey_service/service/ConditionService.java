package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.condition.*;
import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Question;
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
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;

  public ConditionService(
          ConditionRepository conditionRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository
  ) {
    this.conditionRepository = conditionRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
  }

  @Transactional(readOnly = true)
  public List<ConditionResponse> getAll() {
    return conditionRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public List<ConditionResponse> getBySurvey(
          Integer surveyId
  ) {
    return conditionRepository
            .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                    surveyId,
                    surveyId
            )
            .stream()
            .map(this::toResponse)
            .toList();
  }

  public ConditionResponse create(
          CreateConditionRequest request
  ) {
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

    return toResponse(
            conditionRepository.save(condition)
    );
  }

  public ConditionResponse update(
          Integer id,
          UpdateConditionRequest request
  ) {
    Condition condition =
            conditionRepository.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Condition không tồn tại"));

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

    return toResponse(
            conditionRepository.save(condition)
    );
  }

  public void delete(Integer id) {

    Condition condition =
            conditionRepository.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Condition không tồn tại"));

    conditionRepository.delete(condition);
  }

  private Question getQuestion(Integer id) {
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

      LinkedHashSet<Integer> ids =
              new LinkedHashSet<>();

      for (String item : value.split(",")) {

        try {

          int optionId =
                  Integer.parseInt(item.trim());

          if (optionId <= 0) {
            throw new NumberFormatException();
          }

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

        } catch (NumberFormatException e) {

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