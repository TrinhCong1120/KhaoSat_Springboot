package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "validation_rule_type_question_types")
public class ValidationRuleTypeQuestionType {

  @EmbeddedId
  private ValidationRuleTypeQuestionTypeId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("ruleCode")
  @JoinColumn(name = "rule_code")
  private ValidationRuleType ruleType;

  public ValidationRuleTypeQuestionType() {}

  public ValidationRuleTypeQuestionType(ValidationRuleType ruleType, String questionTypeCode) {
    this.ruleType = ruleType;
    this.id = new ValidationRuleTypeQuestionTypeId(ruleType.getCode(), questionTypeCode);
  }

  public ValidationRuleTypeQuestionTypeId getId() { return id; }
  public void setId(ValidationRuleTypeQuestionTypeId id) { this.id = id; }

  public ValidationRuleType getRuleType() { return ruleType; }
  public void setRuleType(ValidationRuleType ruleType) { this.ruleType = ruleType; }

  public String getRuleCode() {
    return id == null ? null : id.getRuleCode();
  }

  public String getQuestionTypeCode() {
    return id == null ? null : id.getQuestionTypeCode();
  }
}
