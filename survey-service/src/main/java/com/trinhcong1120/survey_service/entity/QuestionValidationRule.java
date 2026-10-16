package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_validation_rules")
public class QuestionValidationRule {

  @EmbeddedId
  private QuestionValidationRuleId id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("questionId")
  @JoinColumn(name = "question_id")
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("ruleCode")
  @JoinColumn(name = "rule_code")
  private ValidationRuleType ruleType;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex = 0;

  @JsonIgnore
  @OneToMany(
          mappedBy = "rule",
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL,
          orphanRemoval = true
  )
  private List<QuestionValidationRuleValue> values = new ArrayList<>();

  public QuestionValidationRule() {}

  public QuestionValidationRuleId getId() { return id; }
  public void setId(QuestionValidationRuleId id) { this.id = id; }

  public Question getQuestion() { return question; }
  public void setQuestion(Question question) { this.question = question; }

  public ValidationRuleType getRuleType() { return ruleType; }
  public void setRuleType(ValidationRuleType ruleType) { this.ruleType = ruleType; }

  public String getRuleCode() {
    return id == null ? null : id.getRuleCode();
  }

  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

  public List<QuestionValidationRuleValue> getValues() { return values; }
  public void setValues(List<QuestionValidationRuleValue> values) { this.values = values; }
}
