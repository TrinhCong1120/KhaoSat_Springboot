package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.dashboard.*;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardService {

  private final SurveyRepository surveyRepository;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;

  public DashboardService(
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository
  ) {
    this.surveyRepository = surveyRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
  }

  public DashboardResponse getDashboard() {

    LocalDateTime now =
            LocalDateTime.now();

    List<Survey> surveys =
            surveyRepository.findAll();

    List<Response> responses =
            responseRepository.findAll();

    long openSurveys =
            surveys.stream()
                    .filter(this::isOpen)
                    .count();

    Double openDelta =
            calculateOpenSurveyDelta(
                    surveys,
                    now
            );

    long responsesLast30Days =
            responses.stream()
                    .filter(r ->
                            r.getSubmittedAt() != null
                                    && !r.getSubmittedAt()
                                    .isBefore(
                                            now.minusDays(30)
                                    ))
                    .count();

    long previous30Days =
            responses.stream()
                    .filter(r ->
                            r.getSubmittedAt() != null
                                    && r.getSubmittedAt()
                                    .isBefore(
                                            now.minusDays(30)
                                    )
                                    && !r.getSubmittedAt()
                                    .isBefore(
                                            now.minusDays(60)
                                    ))
                    .count();

    Double responseTrend =
            calculateTrend(
                    responsesLast30Days,
                    previous30Days
            );

    double completionRate =
            responses.isEmpty()
                    ? 0
                    : responses.stream()
                    .mapToDouble(
                            this::calculateCompletion
                    )
                    .average()
                    .orElse(0);

    completionRate =
            Math.max(
                    0,
                    Math.min(100, completionRate)
            );

    List<ActivityResponse> activities =
            buildActivities(
                    responses,
                    LocalDate.now()
            );

    List<SurveyStatusResponse> statuses =
            buildStatuses(surveys);

    List<TopSurveyResponse> topSurveys =
            buildTopSurveys(
                    surveys,
                    responses
            );

    DashboardResponse result =
            new DashboardResponse();

    result.setOpenSurveys(openSurveys);
    result.setOpenSurveysDelta(openDelta);

    result.setTotalResponses(
            (long) responses.size()
    );

    result.setResponsesLast30Days(
            responsesLast30Days
    );

    result.setResponsesTrendPercent(
            responseTrend
    );

    result.setCompletionRate(
            round2(completionRate)
    );

    result.setSatisfactionScore(null);
    result.setSatisfactionTrendDelta(null);

    result.setActivities(activities);
    result.setSurveyStatuses(statuses);
    result.setTopSurveys(topSurveys);

    return result;
  }

  private boolean isOpen(Survey survey) {

    return Boolean.TRUE.equals(
            survey.getIsActive()
    ) && pageRepository
            .countBySurvey_Id(
                    survey.getId()
            ) > 0;
  }

  private Double calculateOpenSurveyDelta(
          List<Survey> surveys,
          LocalDateTime now
  ) {
    long current = surveys.stream()
            .filter(this::isOpen)
            .filter(s ->
                    s.getCreatedAt() != null
                            && !s.getCreatedAt()
                            .isBefore(
                                    now.minusDays(7)
                            ))
            .count();

    long previous = surveys.stream()
            .filter(this::isOpen)
            .filter(s ->
                    s.getCreatedAt() != null
                            && s.getCreatedAt()
                            .isBefore(
                                    now.minusDays(7)
                            )
                            && !s.getCreatedAt()
                            .isBefore(
                                    now.minusDays(14)
                            ))
            .count();

    if (current == 0 && previous == 0) {
      return null;
    }

    if (previous == 0) {
      return 100.0;
    }

    return round2(
            (current - previous)
                    * 100.0
                    / previous
    );
  }

  private Double calculateTrend(
          long current,
          long previous
  ) {
    if (previous == 0) {

      if (current == 0) {
        return null;
      }

      return 100.0;
    }

    return round2(
            (current - previous)
                    * 100.0
                    / previous
    );
  }

  private double calculateCompletion(
          Response response
  ) {
    Integer surveyId =
            response.getSurvey().getId();

    List<Question> requiredQuestions =
            questionRepository
                    .findByPage_Survey_Id(
                            surveyId
                    )
                    .stream()
                    .filter(q ->
                            Boolean.TRUE.equals(
                                    q.getIsRequired()
                            ))
                    .toList();

    if (requiredQuestions.isEmpty()) {
      return 100.0;
    }

    Map<Integer, Answer> answerMap =
            new HashMap<>();

    for (Answer answer :
            answerRepository
                    .findByResponse_Id(
                            response.getId()
                    )) {

      answerMap.put(
              answer.getQuestion().getId(),
              answer
      );
    }

    long completed = 0;

    for (Question question :
            requiredQuestions) {

      Answer answer =
              answerMap.get(question.getId());

      if (hasValidAnswer(
              question,
              answer
      )) {
        completed++;
      }
    }

    return completed
            * 100.0
            / requiredQuestions.size();
  }

  private boolean hasValidAnswer(
          Question question,
          Answer answer
  ) {
    if (answer == null) {
      return false;
    }

    String type =
            question.getQuestionType() == null
                    ? ""
                    : question.getQuestionType().getCode();

    return switch (type) {

      case "SINGLE_CHOICE",
           "MULTIPLE_CHOICE" ->
              answer.getAnswerOptions() != null
                      && !answer.getAnswerOptions()
                      .isEmpty();

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

  private List<ActivityResponse> buildActivities(
          List<Response> responses,
          LocalDate today
  ) {
    List<ActivityResponse> result =
            new ArrayList<>();

    for (int i = 6; i >= 0; i--) {

      LocalDate date =
              today.minusDays(i);

      long count =
              responses.stream()
                      .filter(r ->
                              r.getSubmittedAt()
                                      != null
                                      && r.getSubmittedAt()
                                      .toLocalDate()
                                      .equals(date))
                      .count();

      result.add(
              new ActivityResponse(
                      date,
                      getDayLabel(
                              date.getDayOfWeek()
                      ),
                      count,
                      0L
              )
      );
    }

    return result;
  }

  private String getDayLabel(
          DayOfWeek day
  ) {
    return switch (day) {
      case MONDAY -> "T2";
      case TUESDAY -> "T3";
      case WEDNESDAY -> "T4";
      case THURSDAY -> "T5";
      case FRIDAY -> "T6";
      case SATURDAY -> "T7";
      case SUNDAY -> "CN";
    };
  }

  private List<SurveyStatusResponse> buildStatuses(
          List<Survey> surveys
  ) {
    long open = 0;
    long closed = 0;
    long draft = 0;

    for (Survey survey : surveys) {

      long pages =
              pageRepository.countBySurvey_Id(
                      survey.getId()
              );

      if (!Boolean.TRUE.equals(
              survey.getIsActive())) {

        closed++;

      } else if (pages == 0) {

        draft++;

      } else {

        open++;
      }
    }

    return List.of(
            new SurveyStatusResponse(
                    "Đang mở",
                    open
            ),
            new SurveyStatusResponse(
                    "Đã đóng",
                    closed
            ),
            new SurveyStatusResponse(
                    "Bản nháp",
                    draft
            ),
            new SurveyStatusResponse(
                    "Lên lịch",
                    0L
            )
    );
  }

  private List<TopSurveyResponse> buildTopSurveys(
          List<Survey> surveys,
          List<Response> responses
  ) {
    List<TopSurveyResponse> result =
            new ArrayList<>();

    for (Survey survey : surveys) {

      List<Response> surveyResponses =
              responses.stream()
                      .filter(r ->
                              r.getSurvey() != null
                                      && Objects.equals(
                                      r.getSurvey()
                                              .getId(),
                                      survey.getId()
                              ))
                      .toList();

      if (surveyResponses.isEmpty()) {
        continue;
      }

      double completion =
              surveyResponses.stream()
                      .mapToDouble(
                              this::calculateCompletion
                      )
                      .average()
                      .orElse(0);

      result.add(
              new TopSurveyResponse(
                      survey.getId(),
                      survey.getTitle(),
                      round2(completion),
                      (long) surveyResponses.size()
              )
      );
    }

    result.sort(
            Comparator
                    .comparing(
                            TopSurveyResponse::
                                    getCompletionPercent,
                            Comparator.nullsLast(
                                    Comparator.reverseOrder()
                            )
                    )
                    .thenComparing(
                            TopSurveyResponse::
                                    getResponseCount,
                            Comparator.reverseOrder()
                    )
    );

    return result.stream()
            .limit(4)
            .toList();
  }

  private double round2(double value) {
    return Math.round(value * 100.0)
            / 100.0;
  }
}