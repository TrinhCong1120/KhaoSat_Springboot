package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DashboardReportingPeriodTest {
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final DashboardReportingService service = new DashboardReportingService(jdbc,
      mock(SurveyRepository.class), mock(PageRepository.class), mock(SurveyScheduleService.class));

  @Test
  void allAndOneSidedFiltersUseAnIndependentThirtyDayComparison() {
    Instant earliest = Instant.now().minus(Duration.ofDays(400));
    when(jdbc.queryForObject(anyString(), eq(Timestamp.class))).thenReturn(Timestamp.from(earliest));
    var all = service.period(null, null, "AUTO");
    assertEquals("ALL", all.filterMode());
    assertEquals(earliest, all.from());
    assertEquals("MONTH", all.bucket());
    assertEquals(Duration.ofDays(30), Duration.between(all.trendFrom(), all.trendTo()));
    assertEquals(Duration.ofDays(30), Duration.between(all.previousFrom(), all.previousTo()));

    var fromOnly = service.period(Instant.now().minus(Duration.ofDays(14)), null, "AUTO");
    assertEquals("FROM_ONLY", fromOnly.filterMode());
    assertEquals(Duration.ofDays(30), Duration.between(fromOnly.trendFrom(), fromOnly.trendTo()));

    var toOnly = service.period(null, earliest.plus(Duration.ofDays(31)), "AUTO");
    assertEquals("TO_ONLY", toOnly.filterMode());
    assertEquals(earliest, toOnly.from());
    assertEquals(Duration.ofDays(30), Duration.between(toOnly.trendFrom(), toOnly.trendTo()));
  }

  @Test
  void twoDatesCompareThePreviousPeriodOfEqualLength() {
    Instant to = Instant.now().minus(Duration.ofDays(1));
    Instant from = to.minus(Duration.ofDays(7));
    var period = service.period(from, to, "AUTO");
    assertEquals("BETWEEN", period.filterMode());
    assertEquals("DAY", period.bucket());
    assertEquals(from, period.trendFrom());
    assertEquals(to, period.trendTo());
    assertEquals(from.minus(Duration.ofDays(7)), period.previousFrom());
    assertEquals(from, period.previousTo());
  }

  @Test
  void autoGroupingAndVietnamTodayBoundary() {
    assertEquals("MONTH", service.period(Instant.now().minus(Duration.ofDays(400)),
        null, "AUTO").bucket());
    assertEquals("YEAR", service.period(Instant.now().minus(Duration.ofDays(3654)),
        null, "AUTO").bucket());
    Instant tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
        .plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
    var today = service.period(tomorrow.minus(Duration.ofDays(1)), tomorrow, "AUTO");
    assertFalse(today.to().isAfter(Instant.now()));
    assertTrue(today.from().isBefore(today.to()));
    assertThrows(BadRequestException.class, () -> service.period(
        tomorrow.plus(Duration.ofDays(1)), null, "AUTO"));
  }

  @Test
  void toBeforeFirstRecordProducesEmptyValidRange() {
    when(jdbc.queryForObject(anyString(), eq(Timestamp.class))).thenReturn(
        Timestamp.from(Instant.parse("2026-09-01T00:00:00Z")));
    var period = service.period(null, Instant.parse("2020-01-02T00:00:00Z"), "AUTO");
    assertEquals("TO_ONLY", period.filterMode());
    assertEquals(Duration.ofSeconds(1), Duration.between(period.from(), period.to()));
  }
}
