package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.report.CreateExportLogRequest;
import com.trinhcong1120.survey_service.dto.report.ReportViewDto;
import com.trinhcong1120.survey_service.dto.report.SaveReportViewRequest;
import com.trinhcong1120.survey_service.entity.ReportExportLog;
import com.trinhcong1120.survey_service.entity.ReportView;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.ReportExportLogRepository;
import com.trinhcong1120.survey_service.repository.ReportViewRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ReportViewServiceTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private ReportViewRepository viewRepo;
  private ReportExportLogRepository exportLogRepo;
  private SurveyAccessGuard guard;
  private ReportViewService service;

  private final UUID surveyId = UUID.randomUUID();
  private final UUID creatorId = UUID.randomUUID();
  private final UUID otherViewerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    viewRepo = mock(ReportViewRepository.class);
    exportLogRepo = mock(ReportExportLogRepository.class);
    guard = mock(SurveyAccessGuard.class);
    service = new ReportViewService(viewRepo, exportLogRepo, guard);
    when(guard.userId()).thenReturn(creatorId);
    when(viewRepo.save(any())).thenAnswer(inv -> {
      ReportView v = inv.getArgument(0);
      if (v.getId() == null) v.setId(UUID.randomUUID());
      return v;
    });
  }

  @Test
  void createAndListViewsRespectScopeAndCanEdit() throws Exception {
    SaveReportViewRequest req = new SaveReportViewRequest();
    req.setName("Bao cao diem theo lop");
    req.setScope("SHARED");
    req.setDefinition(JSON.readTree("{\"mode\":\"custom\"}"));
    req.setIsDefault(true);

    ReportViewDto created = service.createView(surveyId, req);
    assertEquals("SHARED", created.scope());
    assertTrue(created.canEdit());
    assertTrue(created.isDefault());
    verify(guard).viewResponses(surveyId);

    ReportView sharedByCreator = new ReportView();
    sharedByCreator.setId(UUID.randomUUID());
    sharedByCreator.setSurveyId(surveyId);
    sharedByCreator.setName("Shared View");
    sharedByCreator.setScope("SHARED");
    sharedByCreator.setCreatedByUserId(creatorId);
    sharedByCreator.setDefinitionJson("{\"mode\":\"overview\"}");

    when(guard.userId()).thenReturn(otherViewerId);
    when(guard.can(surveyId, "EDIT")).thenReturn(false);
    when(guard.can(surveyId, "MANAGE_ACCESS")).thenReturn(false);
    when(viewRepo.findAccessibleBySurvey(surveyId, otherViewerId)).thenReturn(List.of(sharedByCreator));

    List<ReportViewDto> views = service.listViews(surveyId);
    assertEquals(1, views.size());
    assertFalse(views.get(0).canEdit());
  }

  @Test
  void updateViewDeniedForNonCreatorWithoutEditPermission() throws Exception {
    UUID viewId = UUID.randomUUID();
    ReportView existing = new ReportView();
    existing.setId(viewId);
    existing.setSurveyId(surveyId);
    existing.setScope("SHARED");
    existing.setCreatedByUserId(creatorId);
    existing.setDefinitionJson("{\"mode\":\"overview\"}");

    when(viewRepo.findByIdAndSurveyId(viewId, surveyId)).thenReturn(Optional.of(existing));
    when(guard.userId()).thenReturn(otherViewerId);
    when(guard.can(surveyId, "EDIT")).thenReturn(false);
    when(guard.can(surveyId, "MANAGE_ACCESS")).thenReturn(false);

    SaveReportViewRequest req = new SaveReportViewRequest();
    req.setName("Attempt overwrite");
    req.setScope("SHARED");
    req.setDefinition(JSON.readTree("{\"mode\":\"custom\"}"));

    assertThrows(AccessDeniedException.class, () -> service.updateView(surveyId, viewId, req));
    verify(viewRepo, never()).save(any());
  }

  @Test
  void logExportRequiresExportPermissionOnlyWhenPiiIncluded() {
    CreateExportLogRequest nonPiiReq = new CreateExportLogRequest();
    nonPiiReq.setExportFormat("excel");
    nonPiiReq.setReportName("Bang tong hop");
    nonPiiReq.setRowCount(25);
    nonPiiReq.setIncludedPii(false);

    service.logExport(surveyId, nonPiiReq);
    verify(guard).viewResponses(surveyId);
    verify(guard, never()).exportData(surveyId);
    verify(exportLogRepo).save(any(ReportExportLog.class));

    CreateExportLogRequest piiReq = new CreateExportLogRequest();
    piiReq.setExportFormat("CSV");
    piiReq.setReportName("Danh sach PII");
    piiReq.setRowCount(25);
    piiReq.setIncludedPii(true);

    service.logExport(surveyId, piiReq);
    verify(guard).exportData(surveyId);

    CreateExportLogRequest badFmt = new CreateExportLogRequest();
    badFmt.setExportFormat("PDF");
    badFmt.setReportName("Invalid");
    badFmt.setIncludedPii(false);
    assertThrows(BadRequestException.class, () -> service.logExport(surveyId, badFmt));
  }
}

