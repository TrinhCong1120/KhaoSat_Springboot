package com.trinhcong1120.survey_service.dto.validation;

import java.util.ArrayList;
import java.util.List;

public class QuestionValidationRuleResponse {

  private String code;
  private String name;
  private Boolean isActive;
  private Integer orderIndex;
  private String errorMessage;
  private String defaultMessage;
  private List<RuleParameterResponse> parameters = new ArrayList<>();

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

  public String getDefaultMessage() { return defaultMessage; }
  public void setDefaultMessage(String defaultMessage) { this.defaultMessage = defaultMessage; }

  public List<RuleParameterResponse> getParameters() { return parameters; }
  public void setParameters(List<RuleParameterResponse> parameters) { this.parameters = parameters; }
}
