package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.statistics.SurveyStatisticsResponse;
import com.trinhcong1120.survey_service.service.ReportService;
import com.trinhcong1120.survey_service.service.StatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Reports")
public class ReportController {

  private static final MediaType XLSX =
          MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          );

  private final ReportService reportService;
  private final StatisticsService statisticsService;

  public ReportController(
          ReportService reportService,
          StatisticsService statisticsService
  ) {
    this.reportService = reportService;
    this.statisticsService = statisticsService;
  }

  @GetMapping("/survey/{surveyId}/statistics")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<SurveyStatisticsResponse> getStatistics(
          @PathVariable Integer surveyId
  ) {
    return ResponseEntity.ok(
            statisticsService.getStatistics(
                    surveyId
            )
    );
  }

  @PostMapping("/survey/{surveyId}/statistics/filter")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<SurveyStatisticsResponse> getFilteredStatistics(
          @PathVariable Integer surveyId,
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
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<byte[]> exportResponses(
          @PathVariable Integer surveyId
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
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<byte[]> exportAnalysis(
          @PathVariable Integer surveyId
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
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<byte[]> exportFilteredAnalysis(
          @PathVariable Integer surveyId,
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
