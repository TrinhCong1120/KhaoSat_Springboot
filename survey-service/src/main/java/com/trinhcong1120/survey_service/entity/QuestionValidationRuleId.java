package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuestionValidationRuleId implements Serializable {

  @Column(name = "question_id")
  private UUID questionId;

  @Column(name = "rule_code", length = 64)
  private String ruleCode;

  public QuestionValidationRuleId() {}

  public QuestionValidationRuleId(UUID questionId, String ruleCode) {
    this.questionId = questionId;
    this.ruleCode = ruleCode;
  }

  public UUID getQuestionId() { return questionId; }
  public void setQuestionId(UUID questionId) { this.questionId = questionId; }

  public String getRuleCode() { return ruleCode; }
  public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof QuestionValidationRuleId that)) {
      return false;
    }
    return Objects.equals(questionId, that.questionId)
            && Objects.equals(ruleCode, that.ruleCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId, ruleCode);
  }
}
