package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.response.ResponseDetailResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseListResponse;
import com.trinhcong1120.survey_service.dto.response.ResponsePageResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseQuestionResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.util.AnswerValueUtil;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResponseService {

  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final ConditionRepository conditionRepository;

  public ResponseService(
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          ConditionRepository conditionRepository
  ) {
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.conditionRepository = conditionRepository;
  }

  public List<ResponseListResponse> getBySurvey(
          Integer surveyId
  ) {
    return responseRepository
            .findBySurvey_IdOrderBySubmittedAtDesc(
                    surveyId
            )
            .stream()
            .map(this::toListResponse)
            .toList();
  }

  public List<ResponseListResponse> filter(
          Integer surveyId,
          ResponseFilterRequest request
  ) {
    return responseRepository
            .findBySurvey_IdOrderBySubmittedAtDesc(
                    surveyId
            )
            .stream()
            .filter(response ->
                    matchesResponseFilter(
                            response,
                            request
                    )
            )
            .map(this::toListResponse)
            .toList();
  }

  public ResponseDetailResponse getDetail(
          Integer responseId
  ) {
    Response response =
            responseRepository
                    .findById(responseId)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Response not found"));

    Integer surveyId =
            response.getSurvey().getId();

    List<Page> pages =
            pageRepository
                    .findBySurvey_IdOrderByOrderIndexAsc(
                            surveyId
                    );

    List<Question> questions =
            questionRepository
                    .findByPage_Survey_Id(surveyId);

    List<Answer> answers =
            answerRepository
                    .findByResponse_Id(response.getId());

    for (Answer answer : answers) {
      answer.setAnswerOptions(
              answerOptionRepository
                      .findByAnswer_Id(answer.getId())
      );
    }

    List<Condition> conditions =
            conditionRepository
                    .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                            surveyId,
                            surveyId
                    );

    Map<Integer, Answer> answerMap =
            answers.stream()
                    .filter(answer ->
                            answer.getQuestion() != null)
                    .collect(Collectors.toMap(
                            answer -> answer
                                    .getQuestion()
                                    .getId(),
                            answer -> answer,
                            (first, second) -> first
                    ));

    Map<Integer, Boolean> applicability =
            ConditionUtil.calculateApplicability(
                    questions,
                    answers,
                    conditions
            );

    List<ResponsePageResponse> pageResponses =
            pages.stream()
                    .map(page ->
                            toPageResponse(
                                    page,
                                    answerMap,
                                    applicability
                            )
                    )
                    .toList();

    return new ResponseDetailResponse(
            response.getId(),
            surveyId,
            response.getSurvey().getTitle(),
            response.getRequestId(),
            response.getSubmittedAt(),
            pageResponses
    );
  }

  public Response getEntity(Integer responseId) {
    return responseRepository
            .findById(responseId)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Response khong ton tai"));
  }

  private ResponsePageResponse toPageResponse(
          Page page,
          Map<Integer, Answer> answerMap,
          Map<Integer, Boolean> applicability
  ) {
    List<Question> questions =
            questionRepository
                    .findByPage_IdOrderByOrderIndexAsc(
                            page.getId()
                    );

    return new ResponsePageResponse(
            page.getId(),
            page.getTitle(),
            page.getOrderIndex(),
            questions.stream()
                    .map(question ->
                            toQuestionResponse(
                                    question,
                                    answerMap.get(question.getId()),
                                    applicability.getOrDefault(
                                            question.getId(),
                                            true
                                    )
                            )
                    )
                    .toList()
    );
  }

  private ResponseQuestionResponse toQuestionResponse(
          Question question,
          Answer answer,
          Boolean isApplicable
  ) {
    ResponseQuestionResponse result =
            new ResponseQuestionResponse();

    result.setQuestionId(question.getId());
    result.setQuestionText(question.getQuestionText());
    result.setQuestionTypeId(
            question.getQuestionType() == null
                    ? null
                    : question.getQuestionType().getId()
    );
    result.setQuestionTypeCode(
            question.getQuestionType() == null
                    ? null
                    : question.getQuestionType().getCode()
    );
    result.setIsRequired(question.getIsRequired());
    result.setIsApplicable(isApplicable);

    if (answer == null) {
      return result;
    }

    result.setAnswerText(answer.getAnswerText());
    result.setAnswerNumber(answer.getAnswerNumber());
    result.setAnswerDate(answer.getAnswerDate());
    result.setProvinceCode(answer.getProvinceCode());
    result.setWardCode(answer.getWardCode());
    result.setProvince(answer.getProvince());
    result.setWard(answer.getWard());
    result.setFormattedAnswer(
            AnswerValueUtil.formatAnswer(answer)
    );

    List<AnswerOption> answerOptions =
            answer.getAnswerOptions() == null
                    ? List.of()
                    : answer.getAnswerOptions();

    result.setOptionIds(
            answerOptions.stream()
                    .map(AnswerOption::getOption)
                    .filter(Objects::nonNull)
                    .map(Option::getId)
                    .toList()
    );

    result.setOptionTexts(
            answerOptions.stream()
                    .map(AnswerOption::getOption)
                    .filter(Objects::nonNull)
                    .map(Option::getOptionText)
                    .filter(Objects::nonNull)
                    .toList()
    );

    return result;
  }

  private ResponseListResponse toListResponse(
          Response response
  ) {
    List<Answer> answers =
            answerRepository
                    .findByResponse_Id(response.getId());

    for (Answer answer : answers) {
      answer.setAnswerOptions(
              answerOptionRepository
                      .findByAnswer_Id(answer.getId())
      );
    }

    String preview = answers.stream()
            .sorted(Comparator.comparing(answer ->
                    answer.getQuestion() == null
                            ? Integer.MAX_VALUE
                            : answer.getQuestion().getOrderIndex()))
            .filter(answer ->
                    !isChoice(answer))
            .map(AnswerValueUtil::formatAnswer)
            .filter(value ->
                    value != null && !value.isBlank())
            .limit(3)
            .collect(Collectors.joining(" | "));

    return new ResponseListResponse(
            response.getId(),
            response.getSurvey().getId(),
            response.getRequestId(),
            response.getSubmittedAt(),
            preview
    );
  }

  private boolean matchesResponseFilter(
          Response response,
          ResponseFilterRequest request
  ) {
    if (request == null) {
      return true;
    }

    if (request.getFrom() != null
            && response.getSubmittedAt() != null
            && response.getSubmittedAt()
            .isBefore(request.getFrom())) {
      return false;
    }

    if (request.getTo() != null
            && response.getSubmittedAt() != null
            && response.getSubmittedAt()
            .isAfter(request.getTo())) {
      return false;
    }

    if (request.getQuestionId() == null) {
      return true;
    }

    return answerRepository
            .findByResponse_Id(response.getId())
            .stream()
            .filter(answer ->
                    answer.getQuestion() != null
                            && Objects.equals(
                            answer.getQuestion().getId(),
                            request.getQuestionId()
                    ))
            .anyMatch(answer ->
                    matchesAnswerFilter(
                            answer,
                            request
                    )
            );
  }

  private boolean matchesAnswerFilter(
          Answer answer,
          ResponseFilterRequest request
  ) {
    if (request.getOptionId() != null) {
      boolean matched =
              answerOptionRepository
                      .findByAnswer_Id(answer.getId())
                      .stream()
                      .anyMatch(answerOption ->
                              answerOption.getOption() != null
                                      && Objects.equals(
                                      answerOption
                                              .getOption()
                                              .getId(),
                                      request.getOptionId()
                              ));

      if (!matched) {
        return false;
      }
    }

    if (request.getText() != null
            && !request.getText().isBlank()) {
      String text =
              answer.getAnswerText() == null
                      ? ""
                      : answer.getAnswerText();

      if (!text.toLowerCase()
              .contains(
                      request.getText()
                              .trim()
                              .toLowerCase()
              )) {
        return false;
      }
    }

    if (request.getNumber() != null
            && !Objects.equals(
            answer.getAnswerNumber(),
            request.getNumber()
    )) {
      return false;
    }

    if (request.getDate() != null
            && (answer.getAnswerDate() == null
            || !Objects.equals(
            answer.getAnswerDate().toLocalDate(),
            request.getDate()
    ))) {
      return false;
    }

    if (request.getProvince() != null
            && !request.getProvince().isBlank()
            && !equalsIgnoreCase(
            answer.getProvince(),
            request.getProvince()
    )) {
      return false;
    }

    if (request.getWard() != null
            && !request.getWard().isBlank()
            && !equalsIgnoreCase(
            answer.getWard(),
            request.getWard()
    )) {
      return false;
    }

    return true;
  }

  private boolean isChoice(Answer answer) {
    if (answer == null
            || answer.getQuestion() == null
            || answer.getQuestion()
            .getQuestionType() == null
            || answer.getQuestion()
            .getQuestionType()
            .getCode() == null) {
      return false;
    }

    String type =
            answer.getQuestion()
                    .getQuestionType()
                    .getCode();

    return "SINGLE_CHOICE".equalsIgnoreCase(type)
            || "MULTIPLE_CHOICE".equalsIgnoreCase(type);
  }

  private boolean equalsIgnoreCase(
          String left,
          String right
  ) {
    if (left == null || right == null) {
      return false;
    }

    return left.trim()
            .equalsIgnoreCase(
                    right.trim()
            );
  }
}
