package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "report_export_logs")
public class ReportExportLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "survey_id", nullable = false)
  private UUID surveyId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "export_format", nullable = false, length = 20)
  private String exportFormat;

  @Column(name = "report_name", nullable = false, length = 200)
  private String reportName;

  @Column(name = "row_count", nullable = false)
  private int rowCount = 0;

  @Column(name = "included_pii", nullable = false)
  private boolean includedPii = false;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "definition_json", columnDefinition = "jsonb")
  private String definitionJson;

  @Column(name = "exported_at", nullable = false)
  private Instant exportedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getSurveyId() { return surveyId; }
  public void setSurveyId(UUID surveyId) { this.surveyId = surveyId; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public String getExportFormat() { return exportFormat; }
  public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

  public String getReportName() { return reportName; }
  public void setReportName(String reportName) { this.reportName = reportName; }

  public int getRowCount() { return rowCount; }
  public void setRowCount(int rowCount) { this.rowCount = rowCount; }

  public boolean isIncludedPii() { return includedPii; }
  public void setIncludedPii(boolean includedPii) { this.includedPii = includedPii; }

  public String getDefinitionJson() { return definitionJson; }
  public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }

  public Instant getExportedAt() { return exportedAt; }
  public void setExportedAt(Instant exportedAt) { this.exportedAt = exportedAt; }
}

