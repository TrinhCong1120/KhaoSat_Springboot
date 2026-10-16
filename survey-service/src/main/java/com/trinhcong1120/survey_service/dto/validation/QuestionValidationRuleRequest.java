package com.trinhcong1120.survey_service.dto.validation;

import java.util.ArrayList;
import java.util.List;

public class QuestionValidationRuleRequest {

  private String code;
  private Boolean isActive = true;
  private Integer orderIndex = 0;
  private String errorMessage;
  private List<RuleParameterRequest> parameters = new ArrayList<>();

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

  public List<RuleParameterRequest> getParameters() { return parameters; }
  public void setParameters(List<RuleParameterRequest> parameters) { this.parameters = parameters; }
}
