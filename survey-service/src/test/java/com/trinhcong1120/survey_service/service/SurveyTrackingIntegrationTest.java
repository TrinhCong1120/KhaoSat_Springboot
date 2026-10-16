package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import com.trinhcong1120.survey_service.repository.PageRepository;
import java.util.UUID;
import java.util.List;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@Transactional // No test Session/Event survives in the development database.
class SurveyTrackingIntegrationTest {
  @Autowired SurveyTrackingService tracking;
  @Autowired SurveyRepository surveys;
  @Autowired PageRepository pages;
  @Autowired SurveyScheduleService schedule;
  @Autowired JdbcTemplate jdbc;
  @Autowired StatisticsService statistics;
  @Autowired SurveyDashboardService dashboard;

  @Test
  void trafficCountsRetriedViewsOnceAndOnlyConfirmedResponses() {
    Survey survey = new Survey();
    survey.setTitle("Traffic test");
    survey.setShareCode("t" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
    survey.setPublishedAt(schedule.now().minusSeconds(60));
    survey = surveys.saveAndFlush(survey);
    Page page = new Page(); page.setSurvey(survey); page.setTitle("Page 1");
    pages.saveAndFlush(page);
    UUID surveyId = survey.getId();
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        UUID.randomUUID().toString(), null,
        List.of(new SimpleGrantedAuthority("survey_view_all"))));
    try {
      Instant from = schedule.now().minusSeconds(5);
      UUID sessionId = tracking.createSession(surveyId,
          new SurveyTrackingService.CreateSession(UUID.randomUUID(), null)).sessionId();
      var view = new SurveyTrackingService.ClientEvent(UUID.randomUUID(), sessionId,
          "SURVEY_VIEWED", null, null, schedule.now());
      assertTrue(tracking.record(surveyId, view).recorded());
      assertFalse(tracking.record(surveyId, view).recorded());
      tracking.record(surveyId, new SurveyTrackingService.ClientEvent(UUID.randomUUID(), sessionId,
          "SURVEY_VIEWED", null, null, schedule.now()));
      tracking.record(surveyId, new SurveyTrackingService.ClientEvent(UUID.randomUUID(), sessionId,
          "SURVEY_STARTED", null, null, schedule.now()));
      Instant to = schedule.now().plusSeconds(60);
      var before = statistics.getTraffic(surveyId, from, to, "Asia/Ho_Chi_Minh", "HOUR");
      assertEquals(2L, before.totalViews());
      assertEquals(1L, before.uniqueSessions());
      assertEquals(1L, before.startedSessions());
      assertEquals(0L, before.submittedResponses());
      assertEquals(100d, before.viewToStartRate());
      jdbc.update("insert into responses(id,survey_id,request_id,submitted_at,session_id) "
              + "values(?,?,?,?,?)", UUID.randomUUID(), surveyId, UUID.randomUUID(),
          Timestamp.from(schedule.now()), sessionId);
      var after = statistics.getTraffic(surveyId, from, to, "Asia/Ho_Chi_Minh", "HOUR");
      assertEquals(1L, after.submittedResponses());
      assertEquals(1L, after.submittedSessions());
      assertEquals(100d, after.viewToSubmitRate());
      assertEquals(100d, after.startToSubmitRate());
      jdbc.update("insert into responses(id,survey_id,request_id,submitted_at) values(?,?,?,?)",
          UUID.randomUUID(), surveyId, UUID.randomUUID(), Timestamp.from(schedule.now()));
      assertEquals(2L, statistics.getResponseTimeline(surveyId).stream()
          .mapToLong(StatisticsService.ResponseTimelinePoint::responses).sum());
      assertEquals(1L, statistics.getTraffic(surveyId, from, to, "Asia/Ho_Chi_Minh", "HOUR")
          .submittedResponses());
      var dashboardReport = dashboard.report(surveyId, from, Instant.now(), "HOUR");
      @SuppressWarnings("unchecked") var lifetime = (java.util.Map<String, Object>) dashboardReport.get("lifetime");
      @SuppressWarnings("unchecked") var activity = (java.util.Map<String, Object>) dashboardReport.get("activity");
      assertEquals(2L, lifetime.get("views"));
      assertEquals(2L, lifetime.get("submissions"));
      assertEquals(2L, activity.get("views"));
      assertEquals(2L, activity.get("submissions"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void clientEventIsIdempotentAndCannotForgeSubmitSuccess() {
    Survey survey = surveys.findAll().stream()
        .filter(s -> schedule.status(s) == SurveyScheduleService.Status.OPEN)
        .findFirst().orElse(null);
    assumeTrue(survey != null, "No open Survey in this test database");
    UUID requestId = UUID.randomUUID();
    var session = tracking.createSession(survey.getId(),
        new SurveyTrackingService.CreateSession(requestId, null));
    assertEquals(session.sessionId(), tracking.createSession(survey.getId(),
        new SurveyTrackingService.CreateSession(requestId, null)).sessionId());
    UUID eventId = UUID.randomUUID();
    var viewed = new SurveyTrackingService.ClientEvent(eventId, session.sessionId(),
        "SURVEY_VIEWED", null, null, schedule.now());
    assertTrue(tracking.record(survey.getId(), viewed).recorded());
    assertFalse(tracking.record(survey.getId(), viewed).recorded());
    assertThrows(BadRequestException.class, () -> tracking.record(survey.getId(),
        new SurveyTrackingService.ClientEvent(UUID.randomUUID(), session.sessionId(),
            "SURVEY_SUBMITTED", null, null, schedule.now())));
  }

  @Test
  void abandonedSessionMustResumeAndExpiredSessionCannotSubmit() {
    Survey survey = surveys.findAll().stream()
        .filter(s -> schedule.status(s) == SurveyScheduleService.Status.OPEN)
        .findFirst().orElse(null);
    assumeTrue(survey != null, "No open Survey in this test database");
    UUID id = tracking.createSession(survey.getId(),
        new SurveyTrackingService.CreateSession(UUID.randomUUID(), null)).sessionId();
    jdbc.update("update survey_sessions set status='STARTED',started_at=?,last_activity_at=? "
            + "where id=?", Timestamp.from(schedule.now().minusSeconds(3600)),
        Timestamp.from(schedule.now().minusSeconds(3600)), id);
    tracking.expireInactiveSessions();
    assertEquals("ABANDONED", jdbc.queryForObject("select status from survey_sessions where id=?",
        String.class, id));
    assertThrows(BadRequestException.class, () -> tracking.verifySession(survey.getId(), id));
    assertThrows(BadRequestException.class, () -> tracking.record(survey.getId(),
        new SurveyTrackingService.ClientEvent(UUID.randomUUID(), id,
            "QUESTION_ANSWERED", null, null, schedule.now())));
    tracking.record(survey.getId(), new SurveyTrackingService.ClientEvent(UUID.randomUUID(), id,
        "SURVEY_RESUMED", null, null, schedule.now()));
    tracking.verifySession(survey.getId(), id);
    jdbc.update("update survey_sessions set status='EXPIRED' where id=?", id);
    assertThrows(BadRequestException.class, () -> tracking.verifySession(survey.getId(), id));
    assertThrows(BadRequestException.class, () -> tracking.record(survey.getId(),
        new SurveyTrackingService.ClientEvent(UUID.randomUUID(), id,
            "SURVEY_RESUMED", null, null, schedule.now())));
  }

}
