package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.statistics.SurveyStatisticsResponse;
import com.trinhcong1120.survey_service.service.ReportService;
import com.trinhcong1120.survey_service.service.StatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<SurveyStatisticsResponse> getStatistics(
          @PathVariable UUID surveyId
  ) {
    return ResponseEntity.ok(
            statisticsService.getStatistics(
                    surveyId
            )
    );
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
