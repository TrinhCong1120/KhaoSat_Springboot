package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.access.AccessDeniedException;

class SurveyTrafficStatisticsTest {
  private final SurveyAccessGuard guard = mock(SurveyAccessGuard.class);
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final StatisticsService statistics = new StatisticsService(
      mock(SurveyRepository.class), guard, mock(QuestionRepository.class),
      mock(AnswerRepository.class), mock(OptionRepository.class),
      mock(AnswerOptionRepository.class), mock(ResponseRepository.class),
      mock(PageRepository.class), jdbc);
  private final UUID surveyId = UUID.randomUUID();
  private final Instant from = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
  private final Instant to = from.plus(1, ChronoUnit.HOURS);

  @Test
  void noTrackingDoesNotInventZeroConversion() {
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(false);
    var result = statistics.getTraffic(surveyId, from, to, "UTC", "HOUR");
    assertEquals("NO_TRACKING", result.trackingStatus());
    assertNull(result.totalViews());
    assertNull(result.viewToSubmitRate());
    assertTrue(result.timeline().isEmpty());
  }

  @Test
  void viewEventsAndCohortSessionsAreCountedSeparately() throws Exception {
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);
    doAnswer(invocation -> {
      String sql = invocation.getArgument(0);
      RowCallbackHandler handler = invocation.getArgument(1);
      ResultSet row = mock(ResultSet.class);
      when(row.getTimestamp("bucket")).thenReturn(Timestamp.from(from));
      if (sql.startsWith("with viewed")) {
        when(row.getLong("sessions")).thenReturn(1L);
        when(row.getLong("started")).thenReturn(1L);
        when(row.getLong("responses")).thenReturn(1L);
        when(row.getLong("submitted_sessions")).thenReturn(1L);
        when(row.getLong("started_submitted")).thenReturn(1L);
      } else when(row.getLong("views")).thenReturn(2L);
      handler.processRow(row);
      return null;
    }).when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

    var result = statistics.getTraffic(surveyId, from, to, "UTC", "HOUR");
    assertEquals("OBSERVED", result.trackingStatus());
    assertEquals(2L, result.totalViews());
    assertEquals(1L, result.uniqueSessions());
    assertEquals(1L, result.submittedResponses());
    assertEquals(100d, result.viewToStartRate());
    assertEquals(100d, result.viewToSubmitRate());
    assertEquals(2L, result.timeline().get(0).views());
  }

  @Test
  void accessIsCheckedBeforeReadingTracking() {
    doThrow(new AccessDeniedException("denied")).when(guard).view(surveyId);
    assertThrows(AccessDeniedException.class,
        () -> statistics.getTraffic(surveyId, from, to, "UTC", "HOUR"));
    verifyNoInteractions(jdbc);
  }
}
