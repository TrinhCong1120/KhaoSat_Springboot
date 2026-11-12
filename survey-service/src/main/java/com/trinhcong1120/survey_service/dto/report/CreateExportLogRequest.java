package com.trinhcong1120.survey_service.dto.report;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateExportLogRequest {

  @NotBlank(message = "Định dạng xuất không được để trống")
  private String exportFormat;

  @NotBlank(message = "Tên báo cáo không được để trống")
  @Size(max = 200, message = "Tên báo cáo tối đa 200 ký tự")
  private String reportName;

  @Min(value = 0, message = "Số dòng không hợp lệ")
  private int rowCount;

  private boolean includedPii;

  private JsonNode definition;

  public String getExportFormat() { return exportFormat; }
  public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

  public String getReportName() { return reportName; }
  public void setReportName(String reportName) { this.reportName = reportName; }

  public int getRowCount() { return rowCount; }
  public void setRowCount(int rowCount) { this.rowCount = rowCount; }

  public boolean isIncludedPii() { return includedPii; }
  public void setIncludedPii(boolean includedPii) { this.includedPii = includedPii; }

  public JsonNode getDefinition() { return definition; }
  public void setDefinition(JsonNode definition) { this.definition = definition; }
}

