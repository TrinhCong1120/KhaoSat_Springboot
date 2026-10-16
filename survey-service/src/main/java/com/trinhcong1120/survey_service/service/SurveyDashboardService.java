package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.exception.BadRequestException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyDashboardService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final JdbcTemplate jdbc;

  public SurveyDashboardService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Map<String, Object> report(UUID surveyId, Instant from, Instant to, String requestedBucket) {
    Instant now = Instant.now();
    Timestamp first = jdbc.queryForObject("select min(at) from ("
        + "select created_at at time zone 'Asia/Ho_Chi_Minh' at from surveys where id=? "
        + "union all select min(submitted_at) from responses where survey_id=? "
        + "union all select min(received_at) from survey_events where survey_id=?) x",
        Timestamp.class, surveyId, surveyId, surveyId);
    Instant start = from == null ? first == null ? now.minusSeconds(1) : first.toInstant() : from;
    Instant end = to == null || to.isAfter(now) && LocalDate.ofInstant(to.minusNanos(1), ZONE).equals(LocalDate.now(ZONE)) ? now : to;
    if (from == null && !start.isBefore(end)) start = end.minusSeconds(1);
    if (!start.isBefore(end) || start.isAfter(now) || end.isAfter(now))
      throw new BadRequestException("Khoang ngay bao cao khong hop le");
    Duration span = Duration.between(start, end);
    String bucket = requestedBucket == null ? "AUTO" : requestedBucket.toUpperCase(Locale.ROOT);
    if (bucket.equals("AUTO")) bucket = span.compareTo(Duration.ofDays(366)) <= 0 ? "DAY"
        : span.compareTo(Duration.ofDays(3653)) <= 0 ? "MONTH" : "YEAR";
    if (!List.of("HOUR", "DAY", "MONTH", "YEAR").contains(bucket)
        || bucket.equals("HOUR") && span.compareTo(Duration.ofDays(31)) > 0
        || bucket.equals("DAY") && span.compareTo(Duration.ofDays(366)) > 0)
      throw new BadRequestException("Bucket hoac khoang ngay khong hop le");

    Timestamp tracked = jdbc.queryForObject("select min(received_at) from survey_events "
        + "where survey_id=? and event_type='SURVEY_VIEWED' and event_source='CLIENT'",
        Timestamp.class, surveyId);
    long lifetimeSubmissions = count("select count(*) from responses where survey_id=?", surveyId);
    Long lifetimeViews = tracked == null ? null : count("select count(*) from survey_events "
        + "where survey_id=? and event_type='SURVEY_VIEWED' and event_source='CLIENT'", surveyId);
    long submissions = count("select count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<?",
        surveyId, ts(start), ts(end));
    Long views = tracked == null || !tracked.toInstant().isBefore(end) ? null : count(
        "select count(*) from survey_events where survey_id=? and event_type='SURVEY_VIEWED' "
        + "and event_source='CLIENT' and received_at>=? and received_at<?", surveyId, ts(start), ts(end));

    List<Map<String, Object>> points = timeline(surveyId, start, end, bucket, tracked);
    Map<String, Object> activity = map("views", views, "submissions", submissions,
        "timeline", points,
        "trackingStartedAt", tracked == null ? null : tracked.toInstant(),
        "trackingStatus", tracked == null || !tracked.toInstant().isBefore(end) ? "NO_TRACKING"
            : tracked.toInstant().isAfter(start) ? "PARTIAL" : "OBSERVED");
    Instant trendTo = from != null && to != null ? end : now;
    Instant trendFrom = from != null && to != null ? start : now.minus(Duration.ofDays(30));
    Duration trendLength = Duration.between(trendFrom, trendTo);
    Instant previousFrom = trendFrom.minus(trendLength);
    long trendSubmissions = count("select count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<?",
        surveyId, ts(trendFrom), ts(trendTo));
    long previousSubmissions = count("select count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<?",
        surveyId, ts(previousFrom), ts(trendFrom));
    Map<String, Object> viewsTrend = tracked == null || !tracked.toInstant().isBefore(trendFrom) ? null
        : trend(count("select count(*) from survey_events where survey_id=? and event_type='SURVEY_VIEWED' "
                + "and event_source='CLIENT' and received_at>=? and received_at<?",
                surveyId, ts(trendFrom), ts(trendTo)),
            count("select count(*) from survey_events where survey_id=? and event_type='SURVEY_VIEWED' "
                + "and event_source='CLIENT' and received_at>=? and received_at<?",
                surveyId, ts(previousFrom), ts(trendFrom)));
    return map("surveyId", surveyId, "generatedAt", now,
        "period", map("from", start, "to", end, "bucket", bucket, "timezone", ZONE.getId(),
            "filterMode", from == null ? to == null ? "ALL" : "TO_ONLY" : to == null ? "FROM_ONLY" : "BETWEEN",
            "trendMode", from != null && to != null ? "PREVIOUS_EQUAL_PERIOD" : "RECENT_30_DAYS",
            "trendFrom", trendFrom, "trendTo", trendTo, "previousFrom", previousFrom,
            "previousTo", trendFrom),
        "lifetime", map("views", lifetimeViews, "submissions", lifetimeSubmissions),
        "activity", activity, "viewsTrend", viewsTrend,
        "submissionsTrend", trend(trendSubmissions, previousSubmissions),
        "averageSubmissionsPerBucket", points.isEmpty() ? null : round(submissions * 1.0 / points.size()),
        "funnel", funnel(surveyId, start, end), "acquisition", acquisition(surveyId, start, end),
        "engagement", engagement(surveyId, start, end),
        "quality", quality(surveyId, start, end, tracked),
        "completeness", completeness(surveyId, start, end));
  }

  private List<Map<String, Object>> timeline(UUID id, Instant from, Instant to, String bucket, Timestamp tracked) {
    String pattern = switch (bucket) {
      case "HOUR" -> "YYYY-MM-DD HH24";
      case "MONTH" -> "YYYY-MM";
      case "YEAR" -> "YYYY";
      default -> "YYYY-MM-DD";
    };
    String viewsSql = "select to_char(received_at at time zone 'Asia/Ho_Chi_Minh', '" + pattern
        + "'),count(*) from survey_events where survey_id=? and event_type='SURVEY_VIEWED' "
        + "and event_source='CLIENT' and received_at>=? and received_at<? group by 1";
    String submissionsSql = "select to_char(submitted_at at time zone 'Asia/Ho_Chi_Minh', '" + pattern
        + "'),count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<? group by 1";
    Map<String, Long> viewCounts = counts(viewsSql, id, from, to);
    Map<String, Long> submissionCounts = counts(submissionsSql, id, from, to);
    DateTimeFormatter keyFormat = DateTimeFormatter.ofPattern(switch (bucket) {
      case "HOUR" -> "yyyy-MM-dd HH";
      case "MONTH" -> "yyyy-MM";
      case "YEAR" -> "yyyy";
      default -> "yyyy-MM-dd";
    });
    ZonedDateTime cursor = from.atZone(ZONE);
    if (bucket.equals("DAY")) cursor = cursor.toLocalDate().atStartOfDay(ZONE);
    if (bucket.equals("MONTH")) cursor = cursor.toLocalDate().withDayOfMonth(1).atStartOfDay(ZONE);
    if (bucket.equals("YEAR")) cursor = cursor.toLocalDate().withDayOfYear(1).atStartOfDay(ZONE);
    if (bucket.equals("HOUR")) cursor = cursor.withMinute(0).withSecond(0).withNano(0);
    List<Map<String, Object>> points = new ArrayList<>();
    while (cursor.toInstant().isBefore(to)) {
      ZonedDateTime next = switch (bucket) {
        case "HOUR" -> cursor.plusHours(1);
        case "MONTH" -> cursor.plusMonths(1);
        case "YEAR" -> cursor.plusYears(1);
        default -> cursor.plusDays(1);
      };
      String key = cursor.format(keyFormat);
      points.add(map("label", key, "start", cursor.toInstant(),
          "views", tracked == null || !tracked.toInstant().isBefore(next.toInstant()) ? null
              : viewCounts.getOrDefault(key, 0L),
          "submissions", submissionCounts.getOrDefault(key, 0L)));
      cursor = next;
    }
    return points;
  }

  private Map<String, Object> funnel(UUID id, Instant from, Instant to) {
    return jdbc.query("select count(*),"
        + "count(*) filter(where s.started_at is not null and s.started_at<?),"
        + "count(*) filter(where exists(select 1 from responses r where r.session_id=s.id and r.submitted_at<?)),"
        + "count(*) filter(where s.started_at is not null and s.status in ('STARTED','IN_PROGRESS') "
        + "and not exists(select 1 from responses r where r.session_id=s.id)),"
        + "count(*) filter(where s.status='ABANDONED'),"
        + "count(*) filter(where s.started_at is null and not exists "
        + "(select 1 from responses r where r.session_id=s.id)) "
        + "from survey_sessions s where s.survey_id=? and s.first_viewed_at>=? and s.first_viewed_at<? "
        + "and exists(select 1 from survey_events e where e.session_id=s.id "
        + "and e.event_type='SURVEY_VIEWED' and e.event_source='CLIENT')",
        rs -> { rs.next(); long viewed = rs.getLong(1), started = rs.getLong(2), submitted = rs.getLong(3);
          return map("viewed", viewed, "started", started, "submitted", submitted,
              "inProgress", rs.getLong(4), "abandoned", rs.getLong(5), "onlyViewed", rs.getLong(6),
              "viewToStartRate", percent(started, viewed), "startToSubmitRate", percent(submitted, started),
              "viewToSubmitRate", percent(submitted, viewed),
              "provisional", to.isAfter(Instant.now().minusSeconds(1800)));
        }, ts(to), ts(to), id, ts(from), ts(to));
  }

  private Map<String, Object> acquisition(UUID id, Instant from, Instant to) {
    String base = " from survey_sessions where survey_id=? and first_viewed_at>=? and first_viewed_at<?";
    List<Map<String, Object>> devices = distribution("select coalesce(device_type,'UNKNOWN'),count(*)"
        + base + " group by 1 order by 2 desc", id, from, to);
    List<Map<String, Object>> sources = distribution("select coalesce(nullif(utm_source,''),"
        + "nullif(referrer_host,''),'UNKNOWN'),count(*)" + base + " group by 1 order by 2 desc limit 10",
        id, from, to);
    List<Map<String, Object>> mediums = distribution("select coalesce(utm_medium,'UNKNOWN'),count(*)"
        + base + " group by 1 order by 2 desc limit 10", id, from, to);
    return map("devices", devices, "sources", sources, "mediums", mediums);
  }

  private Map<String, Object> engagement(UUID id, Instant from, Instant to) {
    Double median = jdbc.queryForObject("select percentile_cont(0.5) within group(order by s.active_duration_ms) "
        + "from responses r join survey_sessions s on s.id=r.session_id where r.survey_id=? "
        + "and r.submitted_at>=? and r.submitted_at<? and s.active_duration_ms is not null",
        Double.class, id, ts(from), ts(to));
    Double pages = jdbc.queryForObject("select avg(coalesce(v.pages,0)) from survey_sessions s "
        + "left join (select session_id,count(distinct page_id) pages from survey_page_visits group by session_id) v "
        + "on v.session_id=s.id where s.survey_id=? and s.first_viewed_at>=? and s.first_viewed_at<?",
        Double.class, id, ts(from), ts(to));
    List<Map<String, Object>> weekday = jdbc.query("select extract(isodow from submitted_at at time zone "
        + "'Asia/Ho_Chi_Minh')::int,count(*) from responses where survey_id=? and submitted_at>=? "
        + "and submitted_at<? group by 1 order by 1",
        (rs, row) -> map("day", rs.getInt(1), "submissions", rs.getLong(2)), id, ts(from), ts(to));
    List<Map<String, Object>> hour = jdbc.query("select extract(hour from submitted_at at time zone "
        + "'Asia/Ho_Chi_Minh')::int,count(*) from responses where survey_id=? and submitted_at>=? "
        + "and submitted_at<? group by 1 order by 1",
        (rs, row) -> map("hour", rs.getInt(1), "submissions", rs.getLong(2)), id, ts(from), ts(to));
    return map("medianActiveDurationMs", median, "averagePagesViewed", pages,
        "byWeekday", weekday, "byHour", hour);
  }

  private Map<String, Object> quality(UUID id, Instant from, Instant to, Timestamp tracked) {
    long responses = count("select count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<?",
        id, ts(from), ts(to));
    long linked = count("select count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<? "
        + "and session_id is not null", id, ts(from), ts(to));
    long counted = count("select count(*) from responses where survey_id=? and submitted_at>=? and submitted_at<? "
        + "and applicable_question_count is not null", id, ts(from), ts(to));
    long sessions = count("select count(*) from survey_sessions where survey_id=? and first_viewed_at>=? "
        + "and first_viewed_at<?", id, ts(from), ts(to));
    long device = count("select count(*) from survey_sessions where survey_id=? and first_viewed_at>=? "
        + "and first_viewed_at<? and device_type is not null", id, ts(from), ts(to));
    long source = count("select count(*) from survey_sessions where survey_id=? and first_viewed_at>=? "
        + "and first_viewed_at<? and (utm_source is not null or referrer_host is not null)", id, ts(from), ts(to));
    long duration = count("select count(*) from responses r join survey_sessions s on s.id=r.session_id "
        + "where r.survey_id=? and r.submitted_at>=? and r.submitted_at<? and s.active_duration_ms is not null",
        id, ts(from), ts(to));
    return map("responseSessionCoverage", percent(linked, responses),
        "questionCountCoverage", percent(counted, responses), "deviceCoverage", percent(device, sessions),
        "sourceCoverage", percent(source, sessions), "durationCoverage", percent(duration, linked),
        "trackingStartedAt", tracked == null ? null : tracked.toInstant());
  }

  private Map<String, Object> completeness(UUID id, Instant from, Instant to) {
    return jdbc.query("select count(*),count(*) filter(where applicable_question_count is not null "
        + "and answered_question_count is not null),"
        + "coalesce(sum(applicable_question_count) filter(where answered_question_count is not null),0),"
        + "coalesce(sum(answered_question_count) filter(where applicable_question_count is not null),0) "
        + "from responses where survey_id=? and submitted_at>=? and submitted_at<?",
        rs -> { rs.next(); long responses = rs.getLong(1), exact = rs.getLong(2);
          long applicable = rs.getLong(3), answered = rs.getLong(4);
          return map("responses", responses, "exactResponses", exact, "applicableQuestions", applicable,
              "answeredQuestions", answered, "skippedQuestions", applicable - answered,
              "averageApplicable", exact == 0 ? null : round(applicable * 1.0 / exact),
              "averageAnswered", exact == 0 ? null : round(answered * 1.0 / exact),
              "averageSkipped", exact == 0 ? null : round((applicable - answered) * 1.0 / exact),
              "completionRate", percent(answered, applicable), "estimatedLegacyResponses", responses - exact);
        }, id, ts(from), ts(to));
  }

  private Map<String, Long> counts(String sql, UUID id, Instant from, Instant to) {
    Map<String, Long> result = new LinkedHashMap<>();
    jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getString(1), rs.getLong(2)),
        id, ts(from), ts(to));
    return result;
  }

  private List<Map<String, Object>> distribution(String sql, UUID id, Instant from, Instant to) {
    return jdbc.query(sql, (rs, row) -> map("label", rs.getString(1), "count", rs.getLong(2)),
        id, ts(from), ts(to));
  }

  private long count(String sql, Object... args) {
    Long value = jdbc.queryForObject(sql, Long.class, args);
    return value == null ? 0 : value;
  }

  private static Timestamp ts(Instant value) { return Timestamp.from(value); }
  private static Double percent(long numerator, long denominator) {
    return denominator == 0 ? null : round(numerator * 100.0 / denominator);
  }
  private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
  private static Map<String, Object> trend(long current, long previous) {
    return map("delta", current - previous, "percent", previous == 0 ? null
        : round((current - previous) * 100.0 / previous),
        "label", previous == 0 ? current == 0 ? "—" : "Mới" : null);
  }
  private static Map<String, Object> map(Object... pairs) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
    return result;
  }
}
