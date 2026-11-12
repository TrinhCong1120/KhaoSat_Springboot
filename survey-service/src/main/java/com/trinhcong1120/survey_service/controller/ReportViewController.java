package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.report.CreateExportLogRequest;
import com.trinhcong1120.survey_service.dto.report.ReportViewDto;
import com.trinhcong1120.survey_service.dto.report.SaveReportViewRequest;
import com.trinhcong1120.survey_service.service.ReportViewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/Reports")
public class ReportViewController {

  private final ReportViewService reportViewService;

  public ReportViewController(ReportViewService reportViewService) {
    this.reportViewService = reportViewService;
  }

  @GetMapping("/survey/{surveyId}/views")
  public ResponseEntity<List<ReportViewDto>> listViews(@PathVariable UUID surveyId) {
    return ResponseEntity.ok(reportViewService.listViews(surveyId));
  }

  @PostMapping("/survey/{surveyId}/views")
  public ResponseEntity<ReportViewDto> createView(
      @PathVariable UUID surveyId,
      @Valid @RequestBody SaveReportViewRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reportViewService.createView(surveyId, request));
  }

  @PutMapping("/survey/{surveyId}/views/{viewId}")
  public ResponseEntity<ReportViewDto> updateView(
      @PathVariable UUID surveyId,
      @PathVariable UUID viewId,
      @Valid @RequestBody SaveReportViewRequest request) {
    return ResponseEntity.ok(reportViewService.updateView(surveyId, viewId, request));
  }

  @DeleteMapping("/survey/{surveyId}/views/{viewId}")
  public ResponseEntity<Void> deleteView(
      @PathVariable UUID surveyId,
      @PathVariable UUID viewId) {
    reportViewService.deleteView(surveyId, viewId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/survey/{surveyId}/export-logs")
  public ResponseEntity<Void> logExport(
      @PathVariable UUID surveyId,
      @Valid @RequestBody CreateExportLogRequest request) {
    reportViewService.logExport(surveyId, request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}

