package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.trinhcong1120.survey_service.controller.DashboardController;
import com.trinhcong1120.survey_service.controller.ResponseController;
import com.trinhcong1120.survey_service.controller.ReportController;
import com.trinhcong1120.survey_service.controller.SurveyController;
import com.trinhcong1120.survey_service.client.CoreUserClient;
import com.trinhcong1120.survey_service.service.DashboardService;
import com.trinhcong1120.survey_service.service.ResponseService;
import com.trinhcong1120.survey_service.service.ReportService;
import com.trinhcong1120.survey_service.service.StatisticsService;
import com.trinhcong1120.survey_service.service.SurveyService;
import com.trinhcong1120.survey_service.service.SurveyDashboardService;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.Arrays;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(DashboardPermissionTest.Config.class)
class DashboardPermissionTest {
  @Configuration
  @EnableMethodSecurity
  static class Config {
    @Bean DashboardService dashboardService() { return mock(DashboardService.class); }
    @Bean ResponseService responseService() { return mock(ResponseService.class); }
    @Bean ReportService reportService() { return mock(ReportService.class); }
    @Bean StatisticsService statisticsService() { return mock(StatisticsService.class); }
    @Bean SurveyDashboardService surveyDashboardService() { return mock(SurveyDashboardService.class); }
    @Bean SurveyAccessGuard surveyAccessGuard() { return mock(SurveyAccessGuard.class); }
    @Bean SurveyService surveyService() { return mock(SurveyService.class); }
    @Bean DashboardController dashboardController(DashboardService service) {
      return new DashboardController(service);
    }
    @Bean ResponseController responseController(ResponseService service) {
      return new ResponseController(service);
    }
    @Bean ReportController reportController(ReportService reports, StatisticsService statistics,
        SurveyDashboardService dashboardService, SurveyAccessGuard accessGuard) {
      return new ReportController(reports, statistics, dashboardService, accessGuard);
    }
    @Bean SurveyController surveyController(SurveyService service) {
      return new SurveyController(service, mock(CoreUserClient.class));
    }
  }

  @Autowired DashboardController dashboard;
  @Autowired ResponseController responses;
  @Autowired ReportController reports;
  @Autowired SurveyController surveys;
  @Autowired DashboardService dashboardService;
  @Autowired ResponseService responseService;
  @Autowired StatisticsService statisticsService;
  @Autowired SurveyDashboardService surveyDashboardService;
  @Autowired SurveyAccessGuard accessGuard;
  @Autowired SurveyService surveyService;

  @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

  @Test
  void dashboardPermissionDoesNotGrantSurveyResults() {
    UUID surveyId = UUID.randomUUID();
    authenticate("dashboard_view");
    dashboard.getDashboard();
    verify(dashboardService).getDashboard();
    assertThrows(AccessDeniedException.class, () -> responses.getBySurvey(surveyId, 1, null));
    assertThrows(AccessDeniedException.class, () -> reports.getStatistics(surveyId));
    assertThrows(AccessDeniedException.class, () -> reports.getResponseTimeline(surveyId));
    assertThrows(AccessDeniedException.class, () -> reports.getDashboard(surveyId, null, null, "AUTO"));
    assertThrows(AccessDeniedException.class, () -> surveys.getAll());
    assertThrows(AccessDeniedException.class, () -> reports.getTraffic(surveyId,
        Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-02T00:00:00Z"),
        "Asia/Ho_Chi_Minh", "DAY"));
  }

  @Test
  void surveyViewGrantsResultsButNotDashboard() {
    UUID surveyId = UUID.randomUUID();
    authenticate("survey_view");
    responses.getBySurvey(surveyId, 1, null);
    reports.getStatistics(surveyId);
    reports.getResponseTimeline(surveyId);
    reports.getDashboard(surveyId, null, null, "AUTO");
    surveys.getAll();
    Instant from = Instant.parse("2026-10-01T00:00:00Z");
    Instant to = Instant.parse("2026-10-02T00:00:00Z");
    reports.getTraffic(surveyId, from, to, "Asia/Ho_Chi_Minh", "DAY");
    verify(responseService).getBySurvey(surveyId, 1, null);
    verify(statisticsService).getStatistics(surveyId);
    verify(statisticsService).getResponseTimeline(surveyId);
    verify(accessGuard).view(surveyId);
    verify(surveyDashboardService).report(surveyId, null, null, "AUTO");
    verify(surveyService).getAll();
    verify(statisticsService).getTraffic(surveyId, from, to, "Asia/Ho_Chi_Minh", "DAY");
    assertThrows(AccessDeniedException.class, () -> dashboard.getDashboard());
  }

  private void authenticate(String... permissions) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null,
            Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
  }
}
