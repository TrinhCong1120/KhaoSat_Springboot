package com.trinhcong1120.survey_service.controller;

import java.util.UUID;
import java.time.Instant;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.statistics.SurveyStatisticsResponse;
import com.trinhcong1120.survey_service.service.ReportService;
import com.trinhcong1120.survey_service.service.StatisticsService;
import com.trinhcong1120.survey_service.service.SurveyDashboardService;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Reports")
@PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all', 'survey_update_all')")
public class ReportController {

  private static final MediaType XLSX =
          MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          );

  private final ReportService reportService;
  private final StatisticsService statisticsService;
  private final SurveyDashboardService dashboardService;
  private final SurveyAccessGuard accessGuard;

  public ReportController(
          ReportService reportService,
          StatisticsService statisticsService,
          SurveyDashboardService dashboardService,
          SurveyAccessGuard accessGuard
  ) {
    this.reportService = reportService;
    this.statisticsService = statisticsService;
    this.dashboardService = dashboardService;
    this.accessGuard = accessGuard;
  }

  @GetMapping("/survey/{surveyId}/dashboard")
  public Map<String, Object> getDashboard(@PathVariable UUID surveyId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "AUTO") String bucket) {
    accessGuard.view(surveyId);
    return dashboardService.report(surveyId, from, to, bucket);
  }

  @GetMapping("/survey/{surveyId}/statistics")
  public ResponseEntity<SurveyStatisticsResponse> getStatistics(
          @PathVariable UUID surveyId
  ) {
    return ResponseEntity.ok(
            statisticsService.getStatistics(
                    surveyId
            )
    );
  }

  /** Thống kê từng trang: phiên theo tracking và phản hồi có đáp án do BE xác nhận. */
  @GetMapping("/survey/{surveyId}/pages")
  public StatisticsService.SurveyPageStatistics getPageStatistics(@PathVariable UUID surveyId) {
    return statisticsService.getPageStatistics(surveyId);
  }

  @GetMapping("/survey/{surveyId}/traffic")
  public StatisticsService.TrafficResult getTraffic(@PathVariable UUID surveyId,
      @RequestParam @org.springframework.format.annotation.DateTimeFormat(
          iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @org.springframework.format.annotation.DateTimeFormat(
          iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) Instant to,
      @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone,
      @RequestParam(defaultValue = "DAY") String bucket) {
    return statisticsService.getTraffic(surveyId, from, to, timezone, bucket);
  }

  @GetMapping("/survey/{surveyId}/responses/timeline")
  public java.util.List<StatisticsService.ResponseTimelinePoint> getResponseTimeline(
      @PathVariable UUID surveyId) {
    return statisticsService.getResponseTimeline(surveyId);
  }

  @PostMapping("/survey/{surveyId}/statistics/filter")
  public ResponseEntity<SurveyStatisticsResponse> getFilteredStatistics(
          @PathVariable UUID surveyId,
          @RequestBody ResponseFilterRequest request
  ) {
    return ResponseEntity.ok(
            statisticsService.getStatistics(
                    surveyId,
                    request
            )
    );
  }

  @GetMapping({
          "/survey/{surveyId}/responses",
          "/survey/{surveyId}/responses.xlsx"
  })
  public ResponseEntity<byte[]> exportResponses(
          @PathVariable UUID surveyId
  ) {

    byte[] data =
            reportService.exportResponses(
                    surveyId
            );

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"survey_"
                            + surveyId
                            + "_responses.xlsx\""
            )
            .contentType(XLSX)
            .contentLength(data.length)
            .body(data);
  }

  @GetMapping({
          "/survey/{surveyId}/analysis",
          "/survey/{surveyId}/analysis.xlsx"
  })
  public ResponseEntity<byte[]> exportAnalysis(
          @PathVariable UUID surveyId
  ) {

    byte[] data =
            reportService.exportAnalysis(
                    surveyId
            );

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"survey_"
                            + surveyId
                            + "_analysis.xlsx\""
            )
            .contentType(XLSX)
            .contentLength(data.length)
            .body(data);
  }

  @PostMapping("/survey/{surveyId}/analysis/filter")
  public ResponseEntity<byte[]> exportFilteredAnalysis(
          @PathVariable UUID surveyId,
          @RequestBody ResponseFilterRequest request
  ) {

    byte[] data =
            reportService.exportAnalysis(
                    surveyId,
                    request
            );

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"survey_"
                            + surveyId
                            + "_analysis.xlsx\""
            )
            .contentType(XLSX)
            .contentLength(data.length)
            .body(data);
  }
}
