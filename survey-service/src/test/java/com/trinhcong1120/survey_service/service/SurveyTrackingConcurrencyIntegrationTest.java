package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.trinhcong1120.survey_service.repository.SurveyRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class SurveyTrackingConcurrencyIntegrationTest {
  @Autowired SurveyTrackingService tracking;
  @Autowired SurveyRepository surveys;
  @Autowired SurveyScheduleService schedule;
  @Autowired JdbcTemplate jdbc;

  @Test
  void simultaneousRetriesKeepOneSessionAndOneEvent() throws Exception {
    var survey = surveys.findAll().stream()
        .filter(s -> schedule.status(s) == SurveyScheduleService.Status.OPEN)
        .findFirst().orElse(null);
    assumeTrue(survey != null, "No open Survey in this test database");
    UUID requestId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    ExecutorService workers = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch start = new CountDownLatch(1);
      Future<UUID> first = workers.submit(() -> {
        start.await();
        return tracking.createSession(survey.getId(),
            new SurveyTrackingService.CreateSession(requestId, null)).sessionId();
      });
      Future<UUID> second = workers.submit(() -> {
        start.await();
        return tracking.createSession(survey.getId(),
            new SurveyTrackingService.CreateSession(requestId, null)).sessionId();
      });
      start.countDown();
      UUID sessionId = first.get(20, TimeUnit.SECONDS);
      assertEquals(sessionId, second.get(20, TimeUnit.SECONDS));
      CountDownLatch eventStart = new CountDownLatch(1);
      var event = new SurveyTrackingService.ClientEvent(eventId, sessionId,
          "SURVEY_VIEWED", null, null, schedule.now());
      Future<Boolean> eventA = workers.submit(() -> {
        eventStart.await();
        return tracking.record(survey.getId(), event).recorded();
      });
      Future<Boolean> eventB = workers.submit(() -> {
        eventStart.await();
        return tracking.record(survey.getId(), event).recorded();
      });
      eventStart.countDown();
      assertNotEquals(eventA.get(20, TimeUnit.SECONDS), eventB.get(20, TimeUnit.SECONDS));
      assertEquals(1L, jdbc.queryForObject("select count(*) from survey_sessions "
          + "where creation_request_id=?", Long.class, requestId));
      assertEquals(1L, jdbc.queryForObject("select count(*) from survey_events "
          + "where event_id=?", Long.class, eventId));
    } finally {
      workers.shutdownNow();
      jdbc.update("delete from survey_events where event_id=?", eventId);
      jdbc.update("delete from survey_sessions where creation_request_id=?", requestId);
    }
  }
}
