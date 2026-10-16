package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

class SurveyDashboardServiceTest {
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void noTrackingKeepsViewMetricsUnknownAndConfirmedResponsesZero() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(anyString(), eq(Timestamp.class), any(Object[].class))).thenReturn(null);
    when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
    when(jdbc.queryForObject(anyString(), eq(Double.class), any(Object[].class))).thenReturn(null);
    when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
    ResultSet rows = mock(ResultSet.class);
    when(rows.next()).thenReturn(true);
    when(jdbc.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
        .thenAnswer(call -> ((ResultSetExtractor<?>) call.getArgument(1)).extractData(rows));

    Map<String, Object> result = new SurveyDashboardService(jdbc).report(UUID.randomUUID(),
        null, null, "AUTO");
    Map<String, Object> lifetime = (Map<String, Object>) result.get("lifetime");
    Map<String, Object> activity = (Map<String, Object>) result.get("activity");
    Map<String, Object> completeness = (Map<String, Object>) result.get("completeness");
    assertNull(lifetime.get("views"));
    assertEquals(0L, lifetime.get("submissions"));
    assertNull(activity.get("views"));
    assertEquals("NO_TRACKING", activity.get("trackingStatus"));
    assertNull(completeness.get("completionRate"));
  }
}
