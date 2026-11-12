package com.trinhcong1120.survey_service.dto.report;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SaveReportViewRequest {

  @NotBlank(message = "Tên mẫu báo cáo không được để trống")
  @Size(max = 200, message = "Tên mẫu báo cáo tối đa 200 ký tự")
  private String name;

  @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
  private String description;

  private String scope = "PRIVATE";

  @NotNull(message = "Cấu hình báo cáo không được để trống")
  private JsonNode definition;

  private boolean isDefault = false;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }

  public JsonNode getDefinition() { return definition; }
  public void setDefinition(JsonNode definition) { this.definition = definition; }

  public boolean getIsDefault() { return isDefault; }
  public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }
}

