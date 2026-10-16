package com.trinhcong1120.survey_service.dto.validation;

import java.util.ArrayList;
import java.util.List;

public class UpdateQuestionValidationRulesRequest {

  private Long expectedRevision;
  private List<QuestionValidationRuleRequest> rules = new ArrayList<>();

  public Long getExpectedRevision() { return expectedRevision; }
  public void setExpectedRevision(Long expectedRevision) { this.expectedRevision = expectedRevision; }

  public List<QuestionValidationRuleRequest> getRules() { return rules; }
  public void setRules(List<QuestionValidationRuleRequest> rules) { this.rules = rules; }
}
