package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.statistics.*;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

  private final SurveyRepository surveyRepository;
  private final QuestionRepository questionRepository;
  private final AnswerRepository answerRepository;
  private final OptionRepository optionRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final ResponseRepository responseRepository;

  public StatisticsService(
          SurveyRepository surveyRepository,
          QuestionRepository questionRepository,
          AnswerRepository answerRepository,
          OptionRepository optionRepository,
          AnswerOptionRepository answerOptionRepository,
          ResponseRepository responseRepository
  ) {
    this.surveyRepository = surveyRepository;
    this.questionRepository = questionRepository;
    this.answerRepository = answerRepository;
    this.optionRepository = optionRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.responseRepository = responseRepository;
  }

  public SurveyStatisticsResponse getStatistics(
          Integer surveyId
  ) {
    return getStatistics(surveyId, null);
  }

  public SurveyStatisticsResponse getStatistics(
          Integer surveyId,
          ResponseFilterRequest filter
  ) {
    Survey survey = surveyRepository.findById(surveyId)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Survey khong ton tai"));

    List<Response> responses =
            responseRepository
                    .findBySurvey_IdOrderBySubmittedAtDesc(
                            surveyId
                    )
                    .stream()
                    .filter(response ->
                            matchesResponseFilter(
                                    response,
                                    filter
                            ))
                    .toList();

    Set<Integer> responseIds =
            responses.stream()
                    .map(Response::getId)
                    .collect(Collectors.toSet());

    long totalResponses =
            responses.size();

    List<QuestionStatisticsResponse> questions =
            questionRepository
                    .findByPage_Survey_Id(surveyId)
                    .stream()
                    .map(question ->
                            buildQuestionStatistics(
                                    question,
                                    totalResponses,
                                    responseIds
                            ))
                    .toList();

    return new SurveyStatisticsResponse(
            survey.getId(),
            survey.getTitle(),
            totalResponses,
            questions
    );
  }

  private QuestionStatisticsResponse buildQuestionStatistics(
          Question question,
          long totalResponses,
          Set<Integer> responseIds
  ) {
    QuestionStatisticsResponse result =
            new QuestionStatisticsResponse();

    result.setQuestionId(question.getId());
    result.setQuestionText(question.getQuestionText());

    String type =
            question.getQuestionType() == null
                    ? ""
                    : question.getQuestionType().getCode();

    result.setQuestionTypeCode(type);

    List<Answer> answers =
            answerRepository
                    .findByQuestion_Id(question.getId())
                    .stream()
                    .filter(answer ->
                            answer.getResponse() != null
                                    && responseIds.contains(
                                    answer.getResponse().getId()
                            ))
                    .toList();

    switch (type) {

      case "SINGLE_CHOICE",
           "MULTIPLE_CHOICE" ->
              result.setChoiceStatistics(
                      buildChoice(
                              question,
                              totalResponses,
                              responseIds
                      ));

      case "NUMBER" ->
              result.setNumberStatistics(
                      buildNumber(answers));

      case "DATE" ->
              result.setDateStatistics(
                      buildDate(answers));

      case "ADDRESS" ->
              result.setAddressStatistics(
                      buildAddress(answers));

      default ->
              result.setTextStatistics(
                      buildText(answers));
    }

    return result;
  }

  private ChoiceStatisticsResponse buildChoice(
          Question question,
          long totalResponses,
          Set<Integer> responseIds
  ) {
    List<Option> options =
            optionRepository
                    .findByQuestion_IdOrderByOrderIndexAsc(
                            question.getId());

    List<ChoiceStatisticsResponse.OptionStatistic>
            statistics = new ArrayList<>();

    for (Option option : options) {

      long count =
              answerOptionRepository
                      .findByOption_Id(option.getId())
                      .stream()
                      .filter(answerOption ->
                              answerOption.getAnswer() != null
                                      && answerOption
                                      .getAnswer()
                                      .getResponse() != null
                                      && responseIds.contains(
                                      answerOption
                                              .getAnswer()
                                              .getResponse()
                                              .getId()
                              ))
                      .count();

      double percentage =
              totalResponses == 0
                      ? 0
                      : count * 100.0
                      / totalResponses;

      statistics.add(
              new ChoiceStatisticsResponse.OptionStatistic(
                      option.getId(),
                      option.getOptionText(),
                      count,
                      percentage
              )
      );
    }

    return new ChoiceStatisticsResponse(statistics);
  }

  private NumberStatisticsResponse buildNumber(
          List<Answer> answers
  ) {
    List<Double> values =
            answers.stream()
                    .map(Answer::getAnswerNumber)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

    if (values.isEmpty()) {
      return new NumberStatisticsResponse(
              0L, null, null, null, null);
    }

    double average =
            values.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

    int size = values.size();
    double median =
            size % 2 == 0
                    ? (values.get(size / 2 - 1)
                    + values.get(size / 2)) / 2.0
                    : values.get(size / 2);

    return new NumberStatisticsResponse(
            (long) size,
            average,
            values.get(0),
            values.get(size - 1),
            median
    );
  }

  private DateStatisticsResponse buildDate(
          List<Answer> answers
  ) {
    List<LocalDateTime> values =
            answers.stream()
                    .map(Answer::getAnswerDate)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

    if (values.isEmpty()) {
      return new DateStatisticsResponse(
              0L, null, null);
    }

    return new DateStatisticsResponse(
            (long) values.size(),
            values.get(0),
            values.get(values.size() - 1)
    );
  }

  private AddressStatisticsResponse buildAddress(
          List<Answer> answers
  ) {
    AddressStatisticsResponse result =
            new AddressStatisticsResponse();

    List<Answer> valid =
            answers.stream()
                    .filter(answer ->
                            answer.getProvince() != null
                                    && !answer.getProvince().isBlank()
                                    && answer.getWard() != null
                                    && !answer.getWard().isBlank())
                    .toList();

    result.setCount((long) valid.size());

    Map<String, Long> provinces = new HashMap<>();
    Map<String, Long> wards = new HashMap<>();
    Map<String, Long> addresses = new HashMap<>();

    for (Answer answer : valid) {
      provinces.merge(
              answer.getProvince(),
              1L,
              Long::sum);

      wards.merge(
              answer.getWard(),
              1L,
              Long::sum);

      addresses.merge(
              answer.getProvince()
                      + "\u0000"
                      + answer.getWard(),
              1L,
              Long::sum);
    }

    result.setTopProvinces(
            provinces.entrySet()
                    .stream()
                    .sorted(Map.Entry
                            .<String, Long>
                                    comparingByValue()
                            .reversed())
                    .limit(15)
                    .map(entry ->
                            new AddressStatisticsResponse
                                    .LocationItem(
                                    entry.getKey(),
                                    entry.getValue()
                            ))
                    .toList()
    );

    result.setTopWards(
            wards.entrySet()
                    .stream()
                    .sorted(Map.Entry
                            .<String, Long>
                                    comparingByValue()
                            .reversed())
                    .limit(15)
                    .map(entry ->
                            new AddressStatisticsResponse
                                    .LocationItem(
                                    entry.getKey(),
                                    entry.getValue()
                            ))
                    .toList()
    );

    result.setTopAddresses(
            addresses.entrySet()
                    .stream()
                    .sorted(Map.Entry
                            .<String, Long>
                                    comparingByValue()
                            .reversed())
                    .limit(15)
                    .map(entry -> {
                      String[] parts =
                              entry.getKey()
                                      .split(
                                              "\u0000",
                                              2
                                      );

                      return new AddressStatisticsResponse
                              .AddressItem(
                              parts[0],
                              parts[1],
                              entry.getValue()
                      );
                    })
                    .toList()
    );

    return result;
  }

  private TextStatisticsResponse buildText(
          List<Answer> answers
  ) {
    Map<String, Long> values =
            new HashMap<>();

    for (Answer answer : answers) {
      if (answer.getAnswerText() == null
              || answer.getAnswerText()
              .isBlank()) {
        continue;
      }

      values.merge(
              answer.getAnswerText().trim(),
              1L,
              Long::sum);
    }

    List<TextStatisticsResponse.TextItem>
            topAnswers =
            values.entrySet()
                    .stream()
                    .sorted(Map.Entry
                            .<String, Long>
                                    comparingByValue()
                            .reversed())
                    .limit(5)
                    .map(entry ->
                            new TextStatisticsResponse
                                    .TextItem(
                                    entry.getKey(),
                                    entry.getValue()
                            ))
                    .toList();

    long count =
            values.values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .sum();

    return new TextStatisticsResponse(
            count,
            topAnswers
    );
  }

  private boolean matchesResponseFilter(
          Response response,
          ResponseFilterRequest filter
  ) {
    if (filter == null) {
      return true;
    }

    if (filter.getFrom() != null
            && response.getSubmittedAt() != null
            && response.getSubmittedAt()
            .isBefore(filter.getFrom())) {
      return false;
    }

    if (filter.getTo() != null
            && response.getSubmittedAt() != null
            && response.getSubmittedAt()
            .isAfter(filter.getTo())) {
      return false;
    }

    if (filter.getQuestionId() == null) {
      return true;
    }

    return answerRepository
            .findByResponse_Id(response.getId())
            .stream()
            .filter(answer ->
                    answer.getQuestion() != null
                            && Objects.equals(
                            answer.getQuestion().getId(),
                            filter.getQuestionId()
                    ))
            .anyMatch(answer ->
                    matchesAnswerFilter(
                            answer,
                            filter
                    )
            );
  }

  private boolean matchesAnswerFilter(
          Answer answer,
          ResponseFilterRequest filter
  ) {
    if (filter.getOptionId() != null) {
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
                                      filter.getOptionId()
                              ));

      if (!matched) {
        return false;
      }
    }

    if (filter.getText() != null
            && !filter.getText().isBlank()) {
      String text =
              answer.getAnswerText() == null
                      ? ""
                      : answer.getAnswerText();

      if (!text.toLowerCase()
              .contains(
                      filter.getText()
                              .trim()
                              .toLowerCase()
              )) {
        return false;
      }
    }

    if (filter.getNumber() != null
            && !Objects.equals(
            answer.getAnswerNumber(),
            filter.getNumber()
    )) {
      return false;
    }

    if (filter.getDate() != null
            && (answer.getAnswerDate() == null
            || !Objects.equals(
            answer.getAnswerDate().toLocalDate(),
            filter.getDate()
    ))) {
      return false;
    }

    if (filter.getProvince() != null
            && !filter.getProvince().isBlank()
            && !equalsIgnoreCase(
            answer.getProvince(),
            filter.getProvince()
    )) {
      return false;
    }

    if (filter.getWard() != null
            && !filter.getWard().isBlank()
            && !equalsIgnoreCase(
            answer.getWard(),
            filter.getWard()
    )) {
      return false;
    }

    return true;
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
