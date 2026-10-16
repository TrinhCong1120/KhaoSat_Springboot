package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.QuestionRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public tracking is observational. Only the Response DB trigger emits SURVEY_SUBMITTED. */
@Service
public class SurveyTrackingService {
  public record CreateSession(UUID requestId, UUID visitorId, String deviceType,
                              String utmSource, String utmMedium, String utmCampaign,
                              String referrerHost) {
    public CreateSession(UUID requestId, UUID visitorId) {
      this(requestId, visitorId, null, null, null, null, null);
    }
  }
  public record SessionResult(UUID sessionId, UUID surveyId, Instant firstViewedAt,
                              Instant expiresAt, String status) {}
  public record ClientEvent(UUID eventId, UUID sessionId, String eventType,
                            UUID pageId, UUID questionId, Instant clientOccurredAt) {}
  public record EventResult(UUID eventId, boolean recorded) {}
  private record SessionState(UUID surveyId, String status) {}

  private static final Set<String> CLIENT_EVENTS = Set.of("SURVEY_VIEWED", "SURVEY_STARTED",
      "PAGE_VIEWED", "PAGE_COMPLETED", "QUESTION_DISPLAYED", "QUESTION_ANSWERED",
      "QUESTION_SKIPPED", "QUESTION_CONDITION_SKIPPED", "VALIDATION_FAILED",
      "SURVEY_SUBMIT_ATTEMPTED", "SURVEY_SUBMIT_FAILED", "SURVEY_RESUMED",
      "SURVEY_SESSION_ENDED");
  private final SurveyRepository surveys;
  private final PageRepository pages;
  private final QuestionRepository questions;
  private final SurveyScheduleService schedule;
  private final JdbcTemplate jdbc;

  public SurveyTrackingService(SurveyRepository surveys, PageRepository pages,
      QuestionRepository questions, SurveyScheduleService schedule, JdbcTemplate jdbc) {
    this.surveys = surveys;
    this.pages = pages;
    this.questions = questions;
    this.schedule = schedule;
    this.jdbc = jdbc;
  }

  @Transactional
  public SessionResult createSession(UUID surveyId, CreateSession input) {
    Survey survey = surveys.findById(surveyId)
        .orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
    if (input != null && input.requestId() != null) {
      List<SessionResult> existing = jdbc.query("select id,survey_id,first_viewed_at,expires_at,status "
              + "from survey_sessions where creation_request_id=?",
          (rs, row) -> new SessionResult(rs.getObject(1, UUID.class),
              rs.getObject(2, UUID.class), rs.getTimestamp(3).toInstant(),
              rs.getTimestamp(4).toInstant(), rs.getString(5)), input.requestId());
      if (!existing.isEmpty()) {
        if (!existing.get(0).surveyId().equals(surveyId))
          throw new BadRequestException("requestId da duoc dung cho Survey khac");
        return existing.get(0);
      }
    }
    if (schedule.status(survey) != SurveyScheduleService.Status.OPEN)
      throw new BadRequestException("Survey hien khong mo de ghi nhan phien");
    Instant now = schedule.now();
    UUID id = UUID.randomUUID();
    Instant expires = now.plus(Duration.ofMinutes(30));
    String device = input == null ? null : input.deviceType();
    if (device != null && !Set.of("MOBILE", "TABLET", "DESKTOP", "OTHER").contains(device))
      throw new BadRequestException("deviceType khong hop le");
    int inserted = jdbc.update("insert into survey_sessions "
            + "(id,survey_id,creation_request_id,visitor_id,first_viewed_at,last_activity_at,expires_at,status,"
            + "device_type,utm_source,utm_medium,utm_campaign,referrer_host) "
            + "values (?,?,?,?,?,?,?,'VIEWED',?,?,?,?,?) on conflict (creation_request_id) do nothing",
        id, surveyId, input == null ? null : input.requestId(),
        input == null ? null : input.visitorId(), Timestamp.from(now), Timestamp.from(now),
        Timestamp.from(expires), device,
        limited(input == null ? null : input.utmSource(), 100),
        limited(input == null ? null : input.utmMedium(), 100),
        limited(input == null ? null : input.utmCampaign(), 150),
        limited(input == null ? null : input.referrerHost(), 255));
    if (inserted == 0) return createSession(surveyId, input);
    return new SessionResult(id, surveyId, now, expires, "VIEWED");
  }

  @Transactional
  public EventResult record(UUID surveyId, ClientEvent input) {
    if (input == null || input.eventId() == null || input.sessionId() == null || input.eventType() == null
        || !CLIENT_EVENTS.contains(input.eventType()))
      throw new BadRequestException("eventId, sessionId va eventType hop le la bat buoc");
    Boolean duplicate = jdbc.query("select survey_id=? and session_id=? and event_type=? "
            + "and page_id is not distinct from ? and question_id is not distinct from ? "
            + "from survey_events where event_id=?",
        rs -> rs.next() ? rs.getBoolean(1) : null,
        surveyId, input.sessionId(), input.eventType(), input.pageId(), input.questionId(),
        input.eventId());
    if (duplicate != null) {
      if (!duplicate) throw new BadRequestException("eventId da duoc dung cho event khac");
      return new EventResult(input.eventId(), false);
    }
    SessionState session = session(input.sessionId());
    if (!session.surveyId().equals(surveyId))
      throw new BadRequestException("Session khong thuoc Survey");
    if ("COMPLETED".equals(session.status()) || "EXPIRED".equals(session.status()))
      throw new BadRequestException("Session da hoan thanh");
    if ("ABANDONED".equals(session.status()) && !"SURVEY_RESUMED".equals(input.eventType()))
      throw new BadRequestException("Can tiep tuc Session truoc khi ghi event");
    if (input.pageId() != null && !pages.existsByIdAndSurvey_Id(input.pageId(), surveyId))
      throw new BadRequestException("Page khong thuoc Survey");
    if (input.questionId() != null && !questions.existsByIdAndPage_Survey_Id(input.questionId(), surveyId))
      throw new BadRequestException("Question khong thuoc Survey");
    if (input.questionId() != null && input.pageId() != null
        && !questions.existsByIdAndPage_Id(input.questionId(), input.pageId()))
      throw new BadRequestException("Question khong thuoc Page");
    if (input.eventType().startsWith("PAGE_") && input.pageId() == null)
      throw new BadRequestException("pageId bat buoc cho Page event");
    Instant now = schedule.now();
    int inserted = jdbc.update("insert into survey_events (id,event_id,session_id,survey_id,"
            + "event_type,page_id,question_id,client_occurred_at,received_at,event_source) "
            + "values (?,?,?,?,?,?,?,?,?,'CLIENT') on conflict (event_id) do nothing",
        UUID.randomUUID(), input.eventId(), input.sessionId(), surveyId, input.eventType(),
        input.pageId(), input.questionId(), input.clientOccurredAt() == null ? null
            : Timestamp.from(input.clientOccurredAt()), Timestamp.from(now));
    if (inserted == 0) {
      Boolean same = jdbc.queryForObject("select survey_id=? and session_id=? and event_type=? "
              + "and page_id is not distinct from ? and question_id is not distinct from ? "
              + "from survey_events where event_id=?", Boolean.class,
          surveyId, input.sessionId(), input.eventType(), input.pageId(), input.questionId(),
          input.eventId());
      if (!Boolean.TRUE.equals(same)) throw new BadRequestException("eventId da duoc dung cho event khac");
      return new EventResult(input.eventId(), false);
    }
    if ("SURVEY_STARTED".equals(input.eventType()))
      jdbc.update("update survey_sessions set started_at=coalesce(started_at,?), "
              + "status='STARTED',last_activity_at=?,expires_at=? where id=?",
          Timestamp.from(now), Timestamp.from(now), Timestamp.from(now.plus(Duration.ofMinutes(30))),
          input.sessionId());
    else if ("PAGE_VIEWED".equals(input.eventType())) {
      jdbc.update("update survey_sessions set status='IN_PROGRESS',last_activity_at=?,expires_at=? where id=?",
          Timestamp.from(now), Timestamp.from(now.plus(Duration.ofMinutes(30))), input.sessionId());
      jdbc.update("insert into survey_page_visits(id,session_id,survey_id,page_id,entered_at) "
              + "values (?,?,?,?,?)", UUID.randomUUID(), input.sessionId(), surveyId,
          input.pageId(), Timestamp.from(now));
    } else if ("PAGE_COMPLETED".equals(input.eventType())) {
      jdbc.update("update survey_page_visits set completed_at=?,left_at=? where id=(select id "
              + "from survey_page_visits where session_id=? and page_id=? and left_at is null "
              + "order by entered_at desc limit 1)", Timestamp.from(now), Timestamp.from(now),
          input.sessionId(), input.pageId());
      touch(input.sessionId(), now);
    } else if ("SURVEY_RESUMED".equals(input.eventType()))
      jdbc.update("update survey_sessions set status='IN_PROGRESS',last_activity_at=?,expires_at=? where id=?",
          Timestamp.from(now), Timestamp.from(now.plus(Duration.ofMinutes(30))), input.sessionId());
    else touch(input.sessionId(), now);
    return new EventResult(input.eventId(), true);
  }

  @Transactional(readOnly = true)
  public void verifySession(UUID surveyId, UUID sessionId) {
    if (sessionId == null) return;
    SessionState session = session(sessionId);
    if (!session.surveyId().equals(surveyId) || "COMPLETED".equals(session.status())
        || "EXPIRED".equals(session.status()) || "ABANDONED".equals(session.status()))
      throw new BadRequestException("Session khong hop le cho Survey");
  }

  public void recordActiveDuration(UUID sessionId, Long durationMs) {
    if (sessionId == null || durationMs == null) return;
    if (durationMs < 0 || durationMs > Duration.ofHours(24).toMillis())
      throw new BadRequestException("activeDurationMs khong hop le");
    jdbc.update("update survey_sessions set active_duration_ms=least(?, "
        + "greatest(0,extract(epoch from (now()-first_viewed_at))*1000)::bigint) "
        + "where id=?", durationMs, sessionId);
  }

  private static String limited(String value, int max) {
    if (value == null || value.isBlank()) return null;
    String clean = value.trim();
    return clean.length() > max ? clean.substring(0, max) : clean;
  }

  private SessionState session(UUID id) {
    SessionState found = jdbc.query("select survey_id,status from survey_sessions where id=?",
        rs -> rs.next() ? new SessionState(rs.getObject(1, UUID.class), rs.getString(2)) : null, id);
    if (found == null) throw new BadRequestException("Session khong ton tai");
    return found;
  }

  private void touch(UUID id, Instant now) {
    jdbc.update("update survey_sessions set last_activity_at=?,expires_at=? where id=?",
        Timestamp.from(now), Timestamp.from(now.plus(Duration.ofMinutes(30))), id);
  }

  @Scheduled(cron = "0 */15 * * * *", zone = "UTC")
  @Transactional
  public void expireInactiveSessions() {
    jdbc.update("update survey_sessions set status='ABANDONED',abandoned_at=now() where status in "
        + "('STARTED','IN_PROGRESS') and last_activity_at < now() - interval '30 minutes'");
    jdbc.update("update survey_sessions set status='EXPIRED' where status='VIEWED' "
        + "and last_activity_at < now() - interval '30 minutes'");
  }
}
