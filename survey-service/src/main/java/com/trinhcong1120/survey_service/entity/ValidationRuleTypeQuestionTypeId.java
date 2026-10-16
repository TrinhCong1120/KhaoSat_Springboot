package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ValidationRuleTypeQuestionTypeId implements Serializable {

  @Column(name = "rule_code", length = 64)
  private String ruleCode;

  @Column(name = "question_type_code", length = 64)
  private String questionTypeCode;

  public ValidationRuleTypeQuestionTypeId() {}

  public ValidationRuleTypeQuestionTypeId(String ruleCode, String questionTypeCode) {
    this.ruleCode = ruleCode;
    this.questionTypeCode = questionTypeCode;
  }

  public String getRuleCode() { return ruleCode; }
  public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

  public String getQuestionTypeCode() { return questionTypeCode; }
  public void setQuestionTypeCode(String questionTypeCode) { this.questionTypeCode = questionTypeCode; }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValidationRuleTypeQuestionTypeId that)) {
      return false;
    }
    return Objects.equals(ruleCode, that.ruleCode)
            && Objects.equals(questionTypeCode, that.questionTypeCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleCode, questionTypeCode);
  }
}
