package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuestionValidationRuleValueId implements Serializable {

  @Column(name = "question_id")
  private UUID questionId;

  @Column(name = "rule_code", length = 64)
  private String ruleCode;

  @Column(name = "parameter_name", length = 64)
  private String parameterName = "value";

  @Column(name = "group_index")
  private Integer groupIndex = 0;

  @Column(name = "position")
  private Integer position = 0;

  public QuestionValidationRuleValueId() {}

  public QuestionValidationRuleValueId(
          UUID questionId,
          String ruleCode,
          String parameterName,
          Integer groupIndex,
          Integer position
  ) {
    this.questionId = questionId;
    this.ruleCode = ruleCode;
    this.parameterName = parameterName;
    this.groupIndex = groupIndex;
    this.position = position;
  }

  public UUID getQuestionId() { return questionId; }
  public void setQuestionId(UUID questionId) { this.questionId = questionId; }

  public String getRuleCode() { return ruleCode; }
  public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

  public String getParameterName() { return parameterName; }
  public void setParameterName(String parameterName) { this.parameterName = parameterName; }

  public Integer getGroupIndex() { return groupIndex; }
  public void setGroupIndex(Integer groupIndex) { this.groupIndex = groupIndex; }

  public Integer getPosition() { return position; }
  public void setPosition(Integer position) { this.position = position; }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof QuestionValidationRuleValueId that)) {
      return false;
    }
    return Objects.equals(questionId, that.questionId)
            && Objects.equals(ruleCode, that.ruleCode)
            && Objects.equals(parameterName, that.parameterName)
            && Objects.equals(groupIndex, that.groupIndex)
            && Objects.equals(position, that.position);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId, ruleCode, parameterName, groupIndex, position);
  }
}
