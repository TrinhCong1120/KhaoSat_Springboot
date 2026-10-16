package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
public class DashboardReportingService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter HOUR_LABEL = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private final JdbcTemplate jdbc;
  private final SurveyRepository surveys;
  private final PageRepository pages;
  private final SurveyScheduleService schedule;

  public DashboardReportingService(JdbcTemplate jdbc, SurveyRepository surveys,
      PageRepository pages, SurveyScheduleService schedule) {
    this.jdbc = jdbc;
    this.surveys = surveys;
    this.pages = pages;
    this.schedule = schedule;
  }

  public record Period(Instant from, Instant to, String bucket, String filterMode,
      Instant trendFrom, Instant trendTo, Instant previousFrom, Instant previousTo) {}

  public Period period(Instant from, Instant to, String bucket) {
    Instant now = Instant.now();
    String mode = from == null ? to == null ? "ALL" : "TO_ONLY"
        : to == null ? "FROM_ONLY" : "BETWEEN";
    Instant earliest = null;
    if (from == null) {
      Timestamp first = jdbc.queryForObject("select min(at) from ("
          + "select min(created_at at time zone 'Asia/Ho_Chi_Minh') at from surveys "
          + "union all select min(submitted_at) from responses "
          + "union all select min(received_at) from survey_events "
          + "union all select min(first_viewed_at) from survey_sessions "
          + "union all select min(first_seen_at) from site_daily_visitors) x", Timestamp.class);
      earliest = first == null ? now.minusSeconds(1) : first.toInstant();
    }
    Instant start = from == null ? earliest : from;
    Instant end = to == null || to.isAfter(now) &&
        LocalDate.ofInstant(to.minusNanos(1), ZONE).equals(LocalDate.now(ZONE)) ? now : to;
    if (from == null && start != null && end != null && !start.isBefore(end))
      start = end.minusSeconds(1);
    if (start == null || end == null || !start.isBefore(end) || start.isAfter(now) || end.isAfter(now))
      throw new BadRequestException("Khoang ngay bao cao khong hop le");
    String unit = bucket == null ? "AUTO" : bucket.toUpperCase(Locale.ROOT);
    Duration span = Duration.between(start, end);
    if (unit.equals("AUTO")) unit = span.compareTo(Duration.ofDays(366)) <= 0 ? "DAY"
        : span.compareTo(Duration.ofDays(3653)) <= 0 ? "MONTH" : "YEAR";
    if (!List.of("HOUR", "DAY", "MONTH", "YEAR").contains(unit))
      throw new BadRequestException("bucket phai la AUTO, HOUR, DAY, MONTH hoac YEAR");
    if (unit.equals("HOUR") && span.compareTo(Duration.ofDays(31)) > 0)
      throw new BadRequestException("Khoang HOUR toi da 31 ngay");
    if (unit.equals("DAY") && span.compareTo(Duration.ofDays(366)) > 0)
      throw new BadRequestException("Khoang DAY toi da 366 ngay");
    Instant trendTo = mode.equals("BETWEEN") ? end : now;
    Instant trendFrom = mode.equals("BETWEEN") ? start : now.minus(Duration.ofDays(30));
    Instant previousFrom = trendFrom.minus(Duration.between(trendFrom, trendTo));
    return new Period(start, end, unit, mode, trendFrom, trendTo, previousFrom, trendFrom);
  }

  public Map<String, Object> report(Period p) {
    Timestamp from = ts(p.from()), to = ts(p.to());
    List<Survey> all = surveys.findAllByOrderByCreatedAtDesc();
    Map<String, Object> portfolio = portfolio(p, all);
    Timestamp firstTracking = jdbc.queryForObject("select min(received_at) from survey_events "
        + "where event_type='SURVEY_VIEWED' and event_source='CLIENT'", Timestamp.class);
    Map<String, Object> result = map(
        "period", map("from", p.from(), "to", p.to(), "bucket", p.bucket(),
            "timezone", ZONE.getId(), "filterMode", p.filterMode(),
            "trendMode", p.filterMode().equals("BETWEEN") ? "PREVIOUS_EQUAL_PERIOD" : "RECENT_30_DAYS",
            "trendFrom", p.trendFrom(), "trendTo", p.trendTo(),
            "previousFrom", p.previousFrom(), "previousTo", p.previousTo()),
        "generatedAt", Instant.now(), "system", system(), "website", website(p),
        "portfolio", portfolio, "activity", activity(p, firstTracking),
        "funnel", funnel(p), "acquisition", acquisition(p), "engagement", engagement(p),
        "quality", quality(p, firstTracking), "completeness", completeness(p));
    long submissions = count("select count(*) from responses where submitted_at>=? and submitted_at<?", from, to);
    long trendSubmissions = count("select count(*) from responses where submitted_at>=? and submitted_at<?",
        ts(p.trendFrom()), ts(p.trendTo()));
    long previousSubmissions = count("select count(*) from responses where submitted_at>=? and submitted_at<?",
        ts(p.previousFrom()), ts(p.previousTo()));
    Timestamp firstView = firstTracking;
    result.put("submissionsTrend", trend(trendSubmissions, previousSubmissions));
    result.put("viewsTrend", firstView == null || !firstView.toInstant().isBefore(p.previousTo()) ? null
        : trend(count("select count(*) from survey_events where event_type='SURVEY_VIEWED' "
              + "and event_source='CLIENT' and received_at>=? and received_at<?",
              ts(p.trendFrom()), ts(p.trendTo())),
            count("select count(*) from survey_events where event_type='SURVEY_VIEWED' "
              + "and event_source='CLIENT' and received_at>=? and received_at<?",
              ts(p.previousFrom()), ts(p.previousTo()))));
    result.put("averageSubmissionsPerSurvey", all.isEmpty() ? null : round(submissions * 1.0 / all.size()));
    int buckets = ((List<?>) ((Map<?, ?>) result.get("activity")).get("timeline")).size();
    result.put("averageSubmissionsPerBucket", buckets == 0 ? null : round(submissions * 1.0 / buckets));
    return result;
  }

  private Map<String, Object> system() {
    List<Map<String, Object>> roles = jdbc.query("select r.name, count(distinct ur.user_id) users "
        + "from roles r left join user_roles ur on ur.role_id=r.id group by r.id,r.name order by r.name",
        (rs, n) -> map("name", rs.getString(1), "users", rs.getLong(2)));
    return map("accounts", count("select count(*) from users"),
        "activeAccounts", count("select count(*) from users where is_active is true"),
        "inactiveAccounts", count("select count(*) from users where is_active is not true"),
        "roles", count("select count(*) from roles"),
        "surveyCreators", count("select count(distinct creator_user_id) from surveys "
            + "where creator_user_id is not null"), "roleDistribution", roles);
  }

  private Map<String, Object> website(Period p) {
    LocalDate today = LocalDate.now(ZONE);
    long visitorsToday = count("select count(*) from site_daily_visitors where visit_date=?", today);
    Instant first = firstSiteVisit();
    String expression = bucketExpression("v.first_seen_at", p.bucket());
    Map<Integer, long[]> counts = new HashMap<>();
    String sql = "with firsts as (select visitor_id,min(visit_date) first_day "
        + "from site_daily_visitors group by visitor_id) "
        + "select " + expression + " idx,count(distinct v.visitor_id), "
        + "count(distinct v.visitor_id) filter(where f.first_day>= "
        + (p.bucket().equals("HOUR") ? "(date_trunc('hour',v.first_seen_at at time zone 'Asia/Ho_Chi_Minh'))::date"
            : p.bucket().equals("MONTH") ? "date_trunc('month',v.first_seen_at at time zone 'Asia/Ho_Chi_Minh')::date"
            : p.bucket().equals("YEAR") ? "date_trunc('year',v.first_seen_at at time zone 'Asia/Ho_Chi_Minh')::date"
            : "v.visit_date") + ") "
        + "from site_daily_visitors v join firsts f using(visitor_id) "
        + "where v.first_seen_at>=? and v.first_seen_at<? group by 1";
    org.springframework.jdbc.core.RowCallbackHandler visitorRow = rs -> counts.put(rs.getInt(1),
        new long[]{rs.getLong(2), rs.getLong(3)});
    if (p.bucket().equals("MONTH"))
      jdbc.query(sql, visitorRow, ts(p.from()), ts(p.from()), ts(p.from()), ts(p.to()));
    else jdbc.query(sql, visitorRow, ts(p.from()), ts(p.from()), ts(p.to()));
    List<Map<String, Object>> timeline = new ArrayList<>();
    for (int i = 0; i < bucketSize(p); i++) {
      Instant start = bucketStart(p, i), end = bucketEnd(p, i);
      long[] c = counts.getOrDefault(i, new long[2]);
      boolean available = first != null && first.isBefore(end);
      String label = p.bucket().equals("HOUR") ? HOUR_LABEL.format(start.atZone(ZONE))
          : p.bucket().equals("YEAR") ? String.valueOf(start.atZone(ZONE).getYear())
          : p.bucket().equals("MONTH") ? start.atZone(ZONE).toLocalDate().toString().substring(0, 7)
          : start.atZone(ZONE).toLocalDate().toString();
      timeline.add(map("date", label, "label", label, "start", start,
          "visitors", available ? c[0] : null,
          "newVisitors", available ? c[1] : null,
          "returningVisitors", available ? c[0] - c[1] : null));
    }
    return map("visitorsToday", first == null ? null : visitorsToday,
        "daily", p.bucket().equals("DAY") ? timeline : List.of(),
        "timeline", timeline, "trackingStartedAt", first);
  }

  private Instant firstSiteVisit() {
    Timestamp value = jdbc.queryForObject("select min(first_seen_at) from site_daily_visitors", Timestamp.class);
    return value == null ? null : value.toInstant();
  }

  private Map<String, Object> portfolio(Period p, List<Survey> all) {
    Map<UUID, List<Page>> bySurvey = new HashMap<>();
    for (Page page : pages.findAll())
      bySurvey.computeIfAbsent(page.getSurvey().getId(), ignored -> new ArrayList<>()).add(page);
    Map<String, Long> statuses = new LinkedHashMap<>();
    for (SurveyScheduleService.Status value : SurveyScheduleService.Status.values())
      statuses.put(value.name(), 0L);
    for (Survey survey : all) {
      String status = schedule.status(survey,
          bySurvey.getOrDefault(survey.getId(), List.of()), schedule.now()).name();
      statuses.put(status, statuses.get(status) + 1);
    }
    return map("totalSurveys", all.size(), "statuses", statuses,
        "createdInPeriod", all.stream().filter(s -> s.getCreatedAt() != null &&
            !s.getCreatedAt().atZone(ZONE).toInstant().isBefore(p.from()) &&
            s.getCreatedAt().atZone(ZONE).toInstant().isBefore(p.to())).count(),
        "publishedInPeriod", all.stream().filter(s -> s.getPublishedAt() != null &&
            !s.getPublishedAt().isBefore(p.from()) && s.getPublishedAt().isBefore(p.to())).count(),
        "withoutResponses", count("select count(*) from surveys s where not exists "
            + "(select 1 from responses r where r.survey_id=s.id)"));
  }

  private Map<String, Object> activity(Period p, Timestamp firstTracking) {
    String expression = bucketExpression("event_time", p.bucket());
    Map<Integer, Long> views = bucketCounts("select " + expression + " idx,count(*) total "
        + "from (select received_at event_time from survey_events where event_type='SURVEY_VIEWED' "
        + "and event_source='CLIENT' and received_at>=? and received_at<?) x group by 1",
        p.from(), p.to(), p.bucket());
    Map<Integer, Long> submits = bucketCounts("select " + expression + " idx,count(*) total "
        + "from (select submitted_at event_time from responses where submitted_at>=? "
        + "and submitted_at<?) x group by 1", p.from(), p.to(), p.bucket());
    int size = bucketSize(p);
    List<Map<String, Object>> timeline = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Instant start = bucketStart(p, i);
      Instant end = bucketEnd(p, i);
      String label = p.bucket().equals("HOUR") ? HOUR_LABEL.format(start.atZone(ZONE))
          : p.bucket().equals("YEAR") ? String.valueOf(start.atZone(ZONE).getYear())
          : p.bucket().equals("MONTH") ? start.atZone(ZONE).toLocalDate().toString().substring(0, 7)
          : LocalDate.ofInstant(start, ZONE).toString();
      timeline.add(map("label", label, "start", start, "views",
          firstTracking == null || !firstTracking.toInstant().isBefore(end) ? null
              : views.getOrDefault(i, 0L), "submissions", submits.getOrDefault(i, 0L)));
    }
    boolean trackedInPeriod = firstTracking != null && firstTracking.toInstant().isBefore(p.to());
    return map("views", !trackedInPeriod ? null : views.values().stream().mapToLong(Long::longValue).sum(),
        "submissions", submits.values().stream().mapToLong(Long::longValue).sum(),
        "timeline", timeline, "trackingStartedAt", firstTracking == null ? null : firstTracking.toInstant(),
        "trackingStatus", !trackedInPeriod ? "NO_TRACKING"
            : firstTracking.toInstant().isAfter(p.from()) ? "PARTIAL" : "OBSERVED");
  }

  private String bucketExpression(String column, String unit) {
    if (unit.equals("HOUR")) return "floor(extract(epoch from (" + column
        + " - ?::timestamptz))/3600)::int";
    String local = "(" + column + " at time zone 'Asia/Ho_Chi_Minh')";
    String base = "(?::timestamptz at time zone 'Asia/Ho_Chi_Minh')";
    if (unit.equals("DAY")) return "(" + local + "::date - " + base + "::date)";
    if (unit.equals("MONTH")) return "((extract(year from " + local + ") - extract(year from "
        + base + ")) * 12 + extract(month from " + local + ") - extract(month from "
        + base + "))::int";
    return "(extract(year from " + local + ") - extract(year from " + base + "))::int";
  }

  private int bucketSize(Period p) {
    if (p.bucket().equals("HOUR"))
      return (int) Math.ceil(Duration.between(p.from(), p.to()).toMillis() / 3600000.0);
    LocalDate first = LocalDate.ofInstant(p.from(), ZONE);
    LocalDate last = LocalDate.ofInstant(p.to().minusNanos(1), ZONE);
    return switch (p.bucket()) {
      case "MONTH" -> (last.getYear() - first.getYear()) * 12 + last.getMonthValue() - first.getMonthValue() + 1;
      case "YEAR" -> last.getYear() - first.getYear() + 1;
      default -> (int) (last.toEpochDay() - first.toEpochDay() + 1);
    };
  }

  private Instant bucketStart(Period p, int index) {
    if (p.bucket().equals("HOUR")) return p.from().plusSeconds(index * 3600L);
    LocalDate first = LocalDate.ofInstant(p.from(), ZONE);
    return switch (p.bucket()) {
      case "MONTH" -> first.withDayOfMonth(1).plusMonths(index).atStartOfDay(ZONE).toInstant();
      case "YEAR" -> first.withDayOfYear(1).plusYears(index).atStartOfDay(ZONE).toInstant();
      default -> first.plusDays(index).atStartOfDay(ZONE).toInstant();
    };
  }

  private Instant bucketEnd(Period p, int index) {
    if (p.bucket().equals("HOUR")) return bucketStart(p, index).plusSeconds(3600);
    LocalDate start = LocalDate.ofInstant(bucketStart(p, index), ZONE);
    return switch (p.bucket()) {
      case "MONTH" -> start.plusMonths(1).atStartOfDay(ZONE).toInstant();
      case "YEAR" -> start.plusYears(1).atStartOfDay(ZONE).toInstant();
      default -> start.plusDays(1).atStartOfDay(ZONE).toInstant();
    };
  }

  private Map<Integer, Long> bucketCounts(String sql, Instant from, Instant to, String bucket) {
    Map<Integer, Long> result = new HashMap<>();
    org.springframework.jdbc.core.RowCallbackHandler row = rs -> result.put(rs.getInt(1), rs.getLong(2));
    if (bucket.equals("MONTH")) jdbc.query(sql, row, ts(from), ts(from), ts(from), ts(to));
    else jdbc.query(sql, row, ts(from), ts(from), ts(to));
    return result;
  }

  private Map<String, Object> funnel(Period p) {
    return jdbc.query("select count(*) viewed, "
        + "count(*) filter(where s.started_at is not null and s.started_at<?) started, "
        + "count(*) filter(where exists(select 1 from responses r where r.session_id=s.id "
        + "and r.submitted_at<?)) submitted, "
        + "count(*) filter(where s.started_at is not null and s.status in ('STARTED','IN_PROGRESS') "
        + "and not exists(select 1 from responses r where r.session_id=s.id)) in_progress, "
        + "count(*) filter(where s.status='ABANDONED') abandoned, "
        + "count(*) filter(where s.started_at is null and not exists "
        + "(select 1 from responses r where r.session_id=s.id)) only_viewed "
        + "from survey_sessions s where s.first_viewed_at>=? and s.first_viewed_at<? "
        + "and exists(select 1 from survey_events e where e.session_id=s.id "
        + "and e.event_type='SURVEY_VIEWED' and e.event_source='CLIENT')",
        rs -> { rs.next(); long viewed = rs.getLong(1), started = rs.getLong(2), submitted = rs.getLong(3);
          return map("viewed", viewed, "started", started, "submitted", submitted,
              "inProgress", rs.getLong(4), "abandoned", rs.getLong(5),
              "onlyViewed", rs.getLong(6), "viewToStartRate", percent(started, viewed),
              "startToSubmitRate", percent(submitted, started),
              "viewToSubmitRate", percent(submitted, viewed), "provisional", p.to().isAfter(Instant.now().minusSeconds(1800)));
        }, ts(p.to()), ts(p.to()), ts(p.from()), ts(p.to()));
  }

  private Map<String, Object> acquisition(Period p) {
    String base = " from survey_sessions where first_viewed_at>=? and first_viewed_at<?";
    List<Map<String, Object>> devices = jdbc.query("select coalesce(device_type,'UNKNOWN'),count(*)"
        + base + " group by 1 order by 2 desc", (rs,n) -> map("label",rs.getString(1),"count",rs.getLong(2)),
        ts(p.from()), ts(p.to()));
    List<Map<String, Object>> sources = jdbc.query("select coalesce(nullif(utm_source,''),"
        + "nullif(referrer_host,''),'UNKNOWN'),count(*)" + base + " group by 1 order by 2 desc limit 10",
        (rs,n) -> map("label",rs.getString(1),"count",rs.getLong(2)), ts(p.from()), ts(p.to()));
    List<Map<String, Object>> mediums = jdbc.query("select coalesce(utm_medium,'UNKNOWN'),count(*)"
        + base + " group by 1 order by 2 desc limit 10",
        (rs,n) -> map("label",rs.getString(1),"count",rs.getLong(2)), ts(p.from()), ts(p.to()));
    return map("devices", devices, "sources", sources, "mediums", mediums);
  }

  private Map<String, Object> engagement(Period p) {
    Double medianMs = jdbc.queryForObject("select percentile_cont(0.5) within group "
        + "(order by s.active_duration_ms) from responses r join survey_sessions s "
        + "on s.id=r.session_id where r.submitted_at>=? and r.submitted_at<? "
        + "and s.active_duration_ms is not null", Double.class, ts(p.from()), ts(p.to()));
    Double avgPages = jdbc.queryForObject("select avg(coalesce(v.pages,0)) from survey_sessions s "
        + "left join (select session_id,count(distinct page_id) pages from survey_page_visits "
        + "group by session_id) v on v.session_id=s.id where s.first_viewed_at>=? "
        + "and s.first_viewed_at<?", Double.class, ts(p.from()), ts(p.to()));
    List<Map<String, Object>> weekday = jdbc.query("select extract(isodow from submitted_at at time zone "
        + "'Asia/Ho_Chi_Minh')::int as weekday,count(*) from responses where submitted_at>=? "
        + "and submitted_at<? group by 1 order by 1",
        (rs,n) -> map("day",rs.getInt(1),"submissions",rs.getLong(2)), ts(p.from()), ts(p.to()));
    List<Map<String, Object>> hour = jdbc.query("select extract(hour from submitted_at at time zone "
        + "'Asia/Ho_Chi_Minh')::int as hour_of_day,count(*) from responses where submitted_at>=? "
        + "and submitted_at<? group by 1 order by 1",
        (rs,n) -> map("hour",rs.getInt(1),"submissions",rs.getLong(2)), ts(p.from()), ts(p.to()));
    return map("medianActiveDurationMs", medianMs, "averagePagesViewed", avgPages,
        "byWeekday", weekday, "byHour", hour);
  }

  private Map<String, Object> quality(Period p, Timestamp firstTracking) {
    Timestamp from = ts(p.from()), to = ts(p.to());
    long responses = count("select count(*) from responses where submitted_at>=? and submitted_at<?", from, to);
    long linked = count("select count(*) from responses where submitted_at>=? and submitted_at<? "
        + "and session_id is not null", from, to);
    long counted = count("select count(*) from responses where submitted_at>=? and submitted_at<? "
        + "and applicable_question_count is not null", from, to);
    long sessions = count("select count(*) from survey_sessions where first_viewed_at>=? "
        + "and first_viewed_at<?", from, to);
    long device = count("select count(*) from survey_sessions where first_viewed_at>=? "
        + "and first_viewed_at<? and device_type is not null", from, to);
    long source = count("select count(*) from survey_sessions where first_viewed_at>=? "
        + "and first_viewed_at<? and (utm_source is not null or referrer_host is not null)", from, to);
    long duration = count("select count(*) from responses r join survey_sessions s on s.id=r.session_id "
        + "where r.submitted_at>=? and r.submitted_at<? and s.active_duration_ms is not null", from, to);
    return map("responseSessionCoverage", percent(linked, responses),
        "questionCountCoverage", percent(counted, responses),
        "deviceCoverage", percent(device, sessions), "sourceCoverage", percent(source, sessions),
        "durationCoverage", percent(duration, linked),
        "trackingStartedAt", firstTracking == null ? null : firstTracking.toInstant());
  }

  private Map<String, Object> completeness(Period p) {
    return jdbc.query("select count(*),count(*) filter(where applicable_question_count is not null "
        + "and answered_question_count is not null), "
        + "coalesce(sum(applicable_question_count) filter(where answered_question_count is not null),0), "
        + "coalesce(sum(answered_question_count) filter(where applicable_question_count is not null),0) "
        + "from responses where submitted_at>=? and submitted_at<?",
        rs -> { rs.next(); long responses = rs.getLong(1), exact = rs.getLong(2),
            applicable = rs.getLong(3), answered = rs.getLong(4);
          return map("responses",responses,"exactResponses",exact,
              "applicableQuestions",applicable,
              "answeredQuestions",answered,"skippedQuestions",applicable-answered,
              "averageApplicable",exact == 0 ? null : round(applicable*1.0/exact),
              "averageAnswered",exact == 0 ? null : round(answered*1.0/exact),
              "averageSkipped",exact == 0 ? null : round((applicable-answered)*1.0/exact),
              "completionRate",percent(answered,applicable),
              "estimatedLegacyResponses",responses-exact);
        }, ts(p.from()),ts(p.to()));
  }

  public Map<String, Object> surveyRows(Period p, String search, String status,
      String sort, int page, int size) {
    if (page < 1 || size < 1 || size > 100) throw new BadRequestException("page/size khong hop le");
    Timestamp previous = ts(p.previousFrom()), from = ts(p.from()), to = ts(p.to());
    Timestamp trendFrom = ts(p.trendFrom()), trendTo = ts(p.trendTo());
    Map<UUID, long[]> responseCounts = new HashMap<>();
    jdbc.query("select survey_id,count(*) filter(where submitted_at>=? and submitted_at<?), "
        + "count(*) filter(where submitted_at>=? and submitted_at<?), "
        + "count(*) filter(where submitted_at>=? and submitted_at<?), "
        + "coalesce(sum(applicable_question_count) filter(where submitted_at>=? and submitted_at<? "
        + "and answered_question_count is not null),0), "
        + "coalesce(sum(answered_question_count) filter(where submitted_at>=? and submitted_at<? "
        + "and applicable_question_count is not null),0), "
        + "count(*) filter(where submitted_at>=? and submitted_at<? "
        + "and applicable_question_count is not null and answered_question_count is not null), "
        + "count(*) filter(where submitted_at>=? and submitted_at<? "
        + "and (applicable_question_count is null or answered_question_count is null)) "
        + "from responses where submitted_at>=? and submitted_at<? group by survey_id",
        (org.springframework.jdbc.core.RowCallbackHandler) rs -> responseCounts.put(rs.getObject(1, UUID.class),
            new long[]{rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),
                rs.getLong(6),rs.getLong(7),rs.getLong(8)}),
        from,to,trendFrom,trendTo,previous,ts(p.previousTo()),
        from,to,from,to,from,to,from,to,
        ts(p.from().isBefore(p.previousFrom()) ? p.from() : p.previousFrom()),
        ts(p.to().isAfter(p.trendTo()) ? p.to() : p.trendTo()));
    Map<UUID, long[]> viewCounts = new HashMap<>();
    Map<UUID, Instant> firstViews = new HashMap<>();
    jdbc.query("select survey_id,count(*) filter(where received_at>=? and received_at<?), "
        + "count(*) filter(where received_at>=? and received_at<?), "
        + "count(*) filter(where received_at>=? and received_at<?),min(received_at) "
        + "from survey_events where event_type='SURVEY_VIEWED' and event_source='CLIENT' "
        + "group by survey_id", (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
          UUID id = rs.getObject(1, UUID.class);
          viewCounts.put(id, new long[]{rs.getLong(2),rs.getLong(3),rs.getLong(4)});
          firstViews.put(id, rs.getTimestamp(5).toInstant());
        }, from,to,trendFrom,trendTo,previous,ts(p.previousTo()));
    Map<UUID, long[]> cohort = new HashMap<>();
    jdbc.query("select s.survey_id,count(*),count(*) filter(where exists(select 1 from responses r "
        + "where r.session_id=s.id and r.submitted_at<?)) from survey_sessions s "
        + "where s.first_viewed_at>=? and s.first_viewed_at<? and exists(select 1 "
        + "from survey_events e where e.session_id=s.id and e.event_type='SURVEY_VIEWED' "
        + "and e.event_source='CLIENT') group by s.survey_id",
        (org.springframework.jdbc.core.RowCallbackHandler) rs -> cohort.put(rs.getObject(1, UUID.class), new long[]{rs.getLong(2),rs.getLong(3)}),
        to,from,to);
    Map<UUID, Double> durations = new HashMap<>();
    jdbc.query("select r.survey_id,percentile_cont(0.5) within group(order by s.active_duration_ms) "
        + "from responses r join survey_sessions s on s.id=r.session_id "
        + "where r.submitted_at>=? and r.submitted_at<? and s.active_duration_ms is not null "
        + "group by r.survey_id", (org.springframework.jdbc.core.RowCallbackHandler) rs -> durations.put(rs.getObject(1, UUID.class),rs.getDouble(2)),
        from,to);
    Map<UUID, List<Page>> bySurvey = new HashMap<>();
    for (Page item : pages.findAll()) bySurvey.computeIfAbsent(item.getSurvey().getId(),
        ignored -> new ArrayList<>()).add(item);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Survey survey : surveys.findAllByOrderByCreatedAtDesc()) {
      String state = schedule.status(survey,bySurvey.getOrDefault(survey.getId(),List.of()),schedule.now()).name();
      if (status != null && !status.isBlank() && !state.equalsIgnoreCase(status)) continue;
      if (search != null && !search.isBlank() && (survey.getTitle() == null ||
          !survey.getTitle().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))) continue;
      UUID id = survey.getId();
      long[] r = responseCounts.getOrDefault(id,new long[7]);
      long[] v = viewCounts.getOrDefault(id,new long[3]);
      long[] c = cohort.getOrDefault(id,new long[2]);
      boolean observed = firstViews.containsKey(id) && firstViews.get(id).isBefore(p.to());
      rows.add(map("surveyId",id,"title",survey.getTitle(),"status",state,
          "views",observed ? v[0] : null,"sessions",observed ? c[0] : null,
          "submissions",r[0],"conversionRate",observed ? percent(c[1],c[0]) : null,
          "applicableQuestions",r[3],"answeredQuestions",r[4],
          "skippedQuestions",r[3]-r[4],"questionCountResponses",r[5],
          "estimatedLegacyResponses",r[6],
          "questionCountSource",r[5] == 0 ? "UNAVAILABLE" : r[6] > 0 ? "PARTIAL" : "EXACT",
          "completionRate",percent(r[4],r[3]),
          "medianActiveDurationMs",durations.get(id),
          "viewsTrend",observed && firstViews.get(id).isBefore(p.previousTo()) ? trend(v[1],v[2]) : null,
          "submissionsTrend",trend(r[1],r[2]),
          "trackingStatus",observed ? firstViews.get(id).isAfter(p.from()) ? "PARTIAL" : "OBSERVED" : "NO_TRACKING"));
    }
    Comparator<Map<String,Object>> order = switch (sort == null ? "submissions" : sort) {
      case "title" -> Comparator.comparing(x -> String.valueOf(x.get("title")),String.CASE_INSENSITIVE_ORDER);
      case "views" -> Comparator.comparingLong((Map<String,Object> x) -> x.get("views") instanceof Number n ? n.longValue() : -1).reversed();
      case "completion" -> Comparator.comparingDouble((Map<String,Object> x) -> x.get("completionRate") instanceof Number n ? n.doubleValue() : -1).reversed();
      default -> Comparator.comparingLong((Map<String,Object> x) -> ((Number)x.get("submissions")).longValue()).reversed();
    };
    rows.sort(order.thenComparing(x -> String.valueOf(x.get("surveyId"))));
    int start = Math.min(rows.size(),(page-1)*size), end = Math.min(rows.size(),start+size);
    return map("total",rows.size(),"page",page,"size",size,"rows",rows.subList(start,end),
        "period",map("from",p.from(),"to",p.to(),"bucket",p.bucket(),
            "filterMode",p.filterMode(),"trendMode",p.filterMode().equals("BETWEEN")
                ? "PREVIOUS_EQUAL_PERIOD" : "RECENT_30_DAYS",
            "trendFrom",p.trendFrom(),"trendTo",p.trendTo(),
            "previousFrom",p.previousFrom(),"previousTo",p.previousTo()));
  }

  private Map<String,Object> trend(long current,long previous) {
    return map("delta",current-previous,"percent",previous == 0 ? null : round((current-previous)*100.0/previous),
        "label",previous == 0 ? current == 0 ? "—" : "Mới" : null);
  }

  private long count(String sql, Object... args) {
    Long value = jdbc.queryForObject(sql,Long.class,args);
    return value == null ? 0 : value;
  }

  private static Timestamp ts(Instant value) { return Timestamp.from(value); }
  private static Double percent(long numerator,long denominator) {
    return denominator == 0 ? null : round(numerator*100.0/denominator);
  }
  private static double round(double value) { return Math.round(value*100.0)/100.0; }
  private static Map<String,Object> map(Object... pairs) {
    Map<String,Object> result = new LinkedHashMap<>();
    for (int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]);
    return result;
  }
}
