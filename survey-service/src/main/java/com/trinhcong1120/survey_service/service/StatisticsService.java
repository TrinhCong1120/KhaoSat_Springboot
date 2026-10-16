package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.statistics.*;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

  private final SurveyRepository surveyRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final QuestionRepository questionRepository;
  private final AnswerRepository answerRepository;
  private final OptionRepository optionRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final ResponseRepository responseRepository;
  private final PageRepository pageRepository;
  private final JdbcTemplate jdbc;

  public record PageStatistics(UUID pageId, String title, Integer orderIndex,
      Long viewedSessions, Long completedSessions, long submittedResponsesWithAnswers,
      String trackingStatus) {}
  public record SurveyPageStatistics(UUID surveyId, String countingUnit,
      String trackingStatus, List<PageStatistics> pages) {}
  public record TrafficPoint(Instant bucketStart, Instant bucketEnd, long views,
      long uniqueSessions, long startedSessions, long submittedResponses,
      long submittedSessions) {}
  public record TrafficResult(UUID surveyId, Instant from, Instant to, String timezone,
      String bucket, Long totalViews, Long uniqueSessions, Long startedSessions,
      Long submittedResponses, Long submittedSessions, Double viewToStartRate,
      Double startToSubmitRate, Double viewToSubmitRate, List<TrafficPoint> timeline,
      String trackingStatus, Instant generatedAt) {}
  public record ResponseTimelinePoint(LocalDate date, long responses) {}
  private record TrafficCounts(long sessions, long started,
      long responses, long submittedSessions, long startedSubmittedSessions) {}

  public StatisticsService(
          SurveyRepository surveyRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          QuestionRepository questionRepository,
          AnswerRepository answerRepository,
          OptionRepository optionRepository,
          AnswerOptionRepository answerOptionRepository,
          ResponseRepository responseRepository, PageRepository pageRepository, JdbcTemplate jdbc
  ) {
    this.surveyRepository = surveyRepository;
    this.guard = guard;
    this.questionRepository = questionRepository;
    this.answerRepository = answerRepository;
    this.optionRepository = optionRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.responseRepository = responseRepository;
    this.pageRepository = pageRepository;
    this.jdbc = jdbc;
  }

  /** Số phiên vào/hoàn thành trang là tracking từ FE; phản hồi có câu trả lời là dữ liệu BE xác nhận. */
  public SurveyPageStatistics getPageStatistics(UUID surveyId) {
    guard.view(surveyId);
    List<Page> pages = pageRepository.findBySurvey_IdOrderByOrderIndexAsc(surveyId);
    Map<UUID, long[]> visits = new HashMap<>();
    jdbc.query("select page_id, count(distinct session_id) viewed, "
            + "count(distinct session_id) filter (where completed_at is not null) completed "
            + "from survey_page_visits where survey_id=? group by page_id",
        (org.springframework.jdbc.core.RowCallbackHandler) rs -> visits.put(
            rs.getObject("page_id", UUID.class),
            new long[]{rs.getLong("viewed"), rs.getLong("completed")}), surveyId);
    Map<UUID, Long> submitted = new HashMap<>();
    jdbc.query("select q.page_id, count(distinct r.id) total from responses r "
            + "join answers a on a.response_id=r.id join questions q on q.id=a.question_id "
            + "where r.survey_id=? group by q.page_id",
        (org.springframework.jdbc.core.RowCallbackHandler) rs -> submitted.put(
            rs.getObject("page_id", UUID.class), rs.getLong("total")), surveyId);
    boolean observed = !visits.isEmpty();
    List<PageStatistics> rows = pages.stream().map(page -> {
      long[] counts = visits.get(page.getId());
      return new PageStatistics(page.getId(), page.getTitle(), page.getOrderIndex(),
          observed ? counts == null ? 0L : counts[0] : null,
          observed ? counts == null ? 0L : counts[1] : null,
          submitted.getOrDefault(page.getId(), 0L), observed ? "OBSERVED" : "NO_TRACKING");
    }).toList();
    return new SurveyPageStatistics(surveyId, "SESSIONS_AND_RESPONSES",
        observed ? "OBSERVED" : "NO_TRACKING", rows);
  }

  public List<ResponseTimelinePoint> getResponseTimeline(UUID surveyId) {
    guard.view(surveyId);
    return jdbc.query("select (submitted_at at time zone 'Asia/Ho_Chi_Minh')::date statistic_date, "
            + "count(*) total from responses where survey_id=? and submitted_at >= now() - interval '30 days' "
            + "group by statistic_date order by statistic_date",
        (rs, row) -> new ResponseTimelinePoint(rs.getDate("statistic_date").toLocalDate(),
            rs.getLong("total")), surveyId);
  }

  /** Đếm một cohort Session đã VIEWED; các tỷ lệ không trộn view và submit của hai nhóm phiên khác nhau. */
  public TrafficResult getTraffic(UUID surveyId, Instant from, Instant to, String timezone,
      String bucket) {
    guard.view(surveyId);
    Instant generatedAt = Instant.now();
    if (from == null || to == null || !from.isBefore(to) || !from.isBefore(generatedAt))
      throw new BadRequestException("from/to phai la khoang thoi gian hop le trong qua khu");
    String resolution = bucket == null ? "DAY" : bucket.toUpperCase(Locale.ROOT);
    if (!"DAY".equals(resolution) && !"HOUR".equals(resolution))
      throw new BadRequestException("bucket phai la HOUR hoac DAY");
    ZoneId zone;
    try { zone = ZoneId.of(timezone == null ? "Asia/Ho_Chi_Minh" : timezone); }
    catch (DateTimeException ex) { throw new BadRequestException("timezone khong hop le"); }
    if (!ZoneId.getAvailableZoneIds().contains(zone.getId()))
      throw new BadRequestException("timezone phai la ten IANA/UTC");
    Duration range = Duration.between(from, to);
    if (range.compareTo("HOUR".equals(resolution) ? Duration.ofDays(31) : Duration.ofDays(366)) > 0)
      throw new BadRequestException("Khoang thoi gian qua dai cho bucket da chon");
    Instant dataEnd = to.isBefore(generatedAt) ? to : generatedAt;
    Boolean hasTracking = jdbc.queryForObject("select exists(select 1 from survey_events "
            + "where survey_id=? and event_type='SURVEY_VIEWED' and event_source='CLIENT' "
            + "and received_at<?)",
        Boolean.class, surveyId, Timestamp.from(dataEnd));
    if (!Boolean.TRUE.equals(hasTracking))
      return new TrafficResult(surveyId, from, to, zone.getId(), resolution,
          null, null, null, null, null, null, null, null, List.of(),
          "NO_TRACKING", generatedAt);

    String bucketExpression = "DAY".equals(resolution)
        ? "(c.first_view_at at time zone ?)::date"
        : "date_trunc('hour', c.first_view_at at time zone 'UTC') at time zone 'UTC'";
    String sql = "with viewed as (select session_id,min(received_at) first_view_at "
        + "from survey_events where survey_id=? and event_type='SURVEY_VIEWED' "
        + "and event_source='CLIENT' and session_id is not null "
        + "and received_at>=? and received_at<? group by session_id), "
        + "cohort as (select v.session_id,v.first_view_at, "
        + "(s.started_at is not null and s.started_at<?) started,count(r.id) responses "
        + "from viewed v join survey_sessions s on s.id=v.session_id and s.survey_id=? "
        + "left join responses r on r.session_id=v.session_id and r.survey_id=? "
        + "and r.submitted_at>=v.first_view_at and r.submitted_at<? "
        + "group by v.session_id,v.first_view_at,s.started_at) "
        + "select " + bucketExpression + " bucket,count(*) sessions, "
        + "count(*) filter(where c.started) started,coalesce(sum(c.responses),0) responses, "
        + "count(*) filter(where c.responses>0) submitted_sessions, "
        + "count(*) filter(where c.started and c.responses>0) started_submitted "
        + "from cohort c group by bucket order by bucket";
    List<Object> args = new ArrayList<>(List.of(surveyId, Timestamp.from(from),
        Timestamp.from(dataEnd), Timestamp.from(dataEnd), surveyId, surveyId,
        Timestamp.from(dataEnd)));
    if ("DAY".equals(resolution)) args.add(zone.getId());
    Map<Instant, TrafficCounts> counts = new HashMap<>();
    jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
      Instant start = "DAY".equals(resolution)
          ? rs.getDate("bucket").toLocalDate().atStartOfDay(zone).toInstant()
          : rs.getTimestamp("bucket").toInstant();
      counts.put(start, new TrafficCounts(rs.getLong("sessions"),
          rs.getLong("started"), rs.getLong("responses"),
          rs.getLong("submitted_sessions"), rs.getLong("started_submitted")));
    }, args.toArray());
    String viewBucket = "DAY".equals(resolution)
        ? "(received_at at time zone ?)::date"
        : "date_trunc('hour', received_at at time zone 'UTC') at time zone 'UTC'";
    String viewSql = "select " + viewBucket + " bucket,count(*) views from survey_events "
        + "where survey_id=? and event_type='SURVEY_VIEWED' and event_source='CLIENT' "
        + "and received_at>=? and received_at<? "
        + "group by bucket";
    Object[] viewArgs = "DAY".equals(resolution)
        ? new Object[]{zone.getId(), surveyId, Timestamp.from(from), Timestamp.from(dataEnd)}
        : new Object[]{surveyId, Timestamp.from(from), Timestamp.from(dataEnd)};
    Map<Instant, Long> viewCounts = new HashMap<>();
    jdbc.query(viewSql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
      Instant start = "DAY".equals(resolution)
          ? rs.getDate("bucket").toLocalDate().atStartOfDay(zone).toInstant()
          : rs.getTimestamp("bucket").toInstant();
      viewCounts.put(start, rs.getLong("views"));
    }, viewArgs);
    long views = 0, sessions = 0, started = 0, responses = 0, submitted = 0, startedSubmitted = 0;
    List<TrafficPoint> timeline = new ArrayList<>();
    Instant cursor = "DAY".equals(resolution)
        ? LocalDate.ofInstant(from, zone).atStartOfDay(zone).toInstant()
        : from.truncatedTo(ChronoUnit.HOURS);
    while (cursor.isBefore(dataEnd)) {
      Instant next = "DAY".equals(resolution)
          ? LocalDate.ofInstant(cursor, zone).plusDays(1).atStartOfDay(zone).toInstant()
          : cursor.plus(1, ChronoUnit.HOURS);
      TrafficCounts value = counts.getOrDefault(cursor, new TrafficCounts(0, 0, 0, 0, 0));
      long bucketViews = viewCounts.getOrDefault(cursor, 0L);
      views += bucketViews; sessions += value.sessions(); started += value.started();
      responses += value.responses(); submitted += value.submittedSessions();
      startedSubmitted += value.startedSubmittedSessions();
      timeline.add(new TrafficPoint(cursor, next, bucketViews, value.sessions(),
          value.started(), value.responses(), value.submittedSessions()));
      cursor = next;
    }
    return new TrafficResult(surveyId, from, to, zone.getId(), resolution,
        views, sessions, started, responses, submitted,
        percent(started, sessions), percent(startedSubmitted, started),
        percent(submitted, sessions), timeline, "OBSERVED", generatedAt);
  }

  private static Double percent(long numerator, long denominator) {
    return denominator == 0 ? null : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP).doubleValue();
  }

  public SurveyStatisticsResponse getStatistics(
          UUID surveyId
  ) {
    return getStatistics(surveyId, null);
  }

  public SurveyStatisticsResponse getStatistics(
          UUID surveyId,
          ResponseFilterRequest filter
  ) {
    guard.view(surveyId);
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

    Set<UUID> responseIds =
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
          Set<UUID> responseIds
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
          Set<UUID> responseIds
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
    List<BigDecimal> values =
            answers.stream()
                    .map(Answer::getAnswerNumber)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

    if (values.isEmpty()) {
      return new NumberStatisticsResponse(
              0L, null, null, null, null);
    }

    BigDecimal sum =
            values.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

    double average =
            sum.divide(
                    BigDecimal.valueOf(values.size()),
                    10,
                    RoundingMode.HALF_UP
            ).doubleValue();

    int size = values.size();
    double median =
            size % 2 == 0
                    ? values.get(size / 2 - 1)
                    .add(values.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP)
                    .doubleValue()
                    : values.get(size / 2).doubleValue();

    return new NumberStatisticsResponse(
            (long) size,
            average,
            values.get(0).doubleValue(),
            values.get(size - 1).doubleValue(),
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
