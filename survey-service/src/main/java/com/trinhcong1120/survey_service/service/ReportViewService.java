package com.trinhcong1120.survey_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.report.CreateExportLogRequest;
import com.trinhcong1120.survey_service.dto.report.ReportViewDto;
import com.trinhcong1120.survey_service.dto.report.SaveReportViewRequest;
import com.trinhcong1120.survey_service.entity.ReportExportLog;
import com.trinhcong1120.survey_service.entity.ReportView;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.ReportExportLogRepository;
import com.trinhcong1120.survey_service.repository.ReportViewRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportViewService {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final ReportViewRepository reportViewRepository;
  private final ReportExportLogRepository exportLogRepository;
  private final SurveyAccessGuard guard;

  public ReportViewService(
      ReportViewRepository reportViewRepository,
      ReportExportLogRepository exportLogRepository,
      SurveyAccessGuard guard) {
    this.reportViewRepository = reportViewRepository;
    this.exportLogRepository = exportLogRepository;
    this.guard = guard;
  }

  @Transactional(readOnly = true)
  public List<ReportViewDto> listViews(UUID surveyId) {
    guard.viewResponses(surveyId);
    UUID userId = guard.userId();
    return reportViewRepository.findAccessibleBySurvey(surveyId, userId).stream()
        .map(view -> toDto(view, surveyId, userId))
        .toList();
  }

  @Transactional
  public ReportViewDto createView(UUID surveyId, SaveReportViewRequest request) {
    guard.viewResponses(surveyId);
    UUID userId = guard.userId();
    String scope = normalizeScope(request.getScope());
    String definitionJson = serializeJson(request.getDefinition(), true);

    if (request.getIsDefault()) {
      clearDefaultFlags(surveyId, userId, scope);
    }

    Instant now = Instant.now();
    ReportView entity = new ReportView();
    entity.setSurveyId(surveyId);
    entity.setName(request.getName().trim());
    entity.setDescription(trimToNull(request.getDescription()));
    entity.setScope(scope);
    entity.setDefinitionJson(definitionJson);
    entity.setCreatedByUserId(userId);
    entity.setDefault(request.getIsDefault());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    return toDto(reportViewRepository.save(entity), surveyId, userId);
  }

  @Transactional
  public ReportViewDto updateView(UUID surveyId, UUID viewId, SaveReportViewRequest request) {
    guard.viewResponses(surveyId);
    UUID userId = guard.userId();
    ReportView entity = reportViewRepository.findByIdAndSurveyId(viewId, surveyId)
        .orElseThrow(() -> new NotFoundException("Mẫu báo cáo không tồn tại"));

    if (!canModify(entity, surveyId, userId)) {
      throw new AccessDeniedException("Bạn không có quyền chỉnh sửa mẫu báo cáo này");
    }

    String scope = normalizeScope(request.getScope());
    String definitionJson = serializeJson(request.getDefinition(), true);

    if (request.getIsDefault()) {
      clearDefaultFlags(surveyId, userId, scope);
    }

    entity.setName(request.getName().trim());
    entity.setDescription(trimToNull(request.getDescription()));
    entity.setScope(scope);
    entity.setDefinitionJson(definitionJson);
    entity.setDefault(request.getIsDefault());
    entity.setUpdatedAt(Instant.now());

    return toDto(reportViewRepository.save(entity), surveyId, userId);
  }

  @Transactional
  public void deleteView(UUID surveyId, UUID viewId) {
    guard.viewResponses(surveyId);
    UUID userId = guard.userId();
    ReportView entity = reportViewRepository.findByIdAndSurveyId(viewId, surveyId)
        .orElseThrow(() -> new NotFoundException("Mẫu báo cáo không tồn tại"));

    if (!canModify(entity, surveyId, userId)) {
      throw new AccessDeniedException("Bạn không có quyền xóa mẫu báo cáo này");
    }

    reportViewRepository.delete(entity);
  }

  @Transactional
  public void logExport(UUID surveyId, CreateExportLogRequest request) {
    if (request.isIncludedPii()) {
      guard.exportData(surveyId);
    } else {
      guard.viewResponses(surveyId);
    }

    String format = request.getExportFormat() == null
        ? ""
        : request.getExportFormat().trim().toUpperCase(Locale.ROOT);
    if (!"EXCEL".equals(format) && !"CSV".equals(format)) {
      throw new BadRequestException("exportFormat phải là EXCEL hoặc CSV");
    }

    ReportExportLog log = new ReportExportLog();
    log.setSurveyId(surveyId);
    log.setUserId(guard.userId());
    log.setExportFormat(format);
    log.setReportName(request.getReportName().trim());
    log.setRowCount(Math.max(0, request.getRowCount()));
    log.setIncludedPii(request.isIncludedPii());
    log.setDefinitionJson(serializeJson(request.getDefinition(), false));
    log.setExportedAt(Instant.now());
    exportLogRepository.save(log);
  }

  private void clearDefaultFlags(UUID surveyId, UUID userId, String scope) {
    if ("SHARED".equals(scope)) {
      for (ReportView existing : reportViewRepository.findBySurveyIdAndScopeAndIsDefaultTrue(surveyId, "SHARED")) {
        existing.setDefault(false);
        reportViewRepository.save(existing);
      }
      return;
    }
    for (ReportView existing : reportViewRepository.findBySurveyIdAndCreatedByUserIdAndIsDefaultTrue(surveyId, userId)) {
      existing.setDefault(false);
      reportViewRepository.save(existing);
    }
  }

  private boolean canModify(ReportView view, UUID surveyId, UUID userId) {
    if (view.getCreatedByUserId().equals(userId)) {
      return true;
    }
    return "SHARED".equalsIgnoreCase(view.getScope())
        && (guard.can(surveyId, "EDIT") || guard.can(surveyId, "MANAGE_ACCESS"));
  }

  private ReportViewDto toDto(ReportView view, UUID surveyId, UUID userId) {
    return new ReportViewDto(
        view.getId(),
        view.getSurveyId(),
        view.getName(),
        view.getDescription(),
        view.getScope(),
        parseJson(view.getDefinitionJson()),
        view.getCreatedByUserId(),
        view.isDefault(),
        canModify(view, surveyId, userId),
        view.getCreatedAt(),
        view.getUpdatedAt()
    );
  }

  private static String normalizeScope(String rawScope) {
    if (rawScope == null || rawScope.isBlank()) return "PRIVATE";
    String scope = rawScope.trim().toUpperCase(Locale.ROOT);
    if (!"PRIVATE".equals(scope) && !"SHARED".equals(scope)) {
      throw new BadRequestException("scope phải là PRIVATE hoặc SHARED");
    }
    return scope;
  }

  private static String serializeJson(JsonNode node, boolean required) {
    if (node == null || node.isNull()) {
      if (required) throw new BadRequestException("definition không được rỗng");
      return null;
    }
    try {
      return JSON.writeValueAsString(node);
    } catch (Exception ex) {
      throw new BadRequestException("definition JSON không hợp lệ");
    }
  }

  private static JsonNode parseJson(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return JSON.readTree(raw);
    } catch (Exception ex) {
      return null;
    }
  }

  private static String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

