package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "question_validation_rule_values")
public class QuestionValidationRuleValue {

  @EmbeddedId
  private QuestionValidationRuleValueId id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
          @JoinColumn(
                  name = "question_id",
                  referencedColumnName = "question_id",
                  insertable = false,
                  updatable = false
          ),
          @JoinColumn(
                  name = "rule_code",
                  referencedColumnName = "rule_code",
                  insertable = false,
                  updatable = false
          )
  })
  private QuestionValidationRule rule;

  @Column(name = "text_value", columnDefinition = "TEXT")
  private String textValue;

  @Column(name = "number_value")
  private BigDecimal numberValue;

  @Column(name = "date_value")
  private LocalDate dateValue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "option_id")
  private Option option;

  public QuestionValidationRuleValue() {}

  public QuestionValidationRuleValueId getId() { return id; }
  public void setId(QuestionValidationRuleValueId id) { this.id = id; }

  public QuestionValidationRule getRule() { return rule; }
  public void setRule(QuestionValidationRule rule) { this.rule = rule; }

  public String getParameterName() {
    return id == null ? null : id.getParameterName();
  }

  public Integer getGroupIndex() {
    return id == null ? 0 : id.getGroupIndex();
  }

  public Integer getPosition() {
    return id == null ? 0 : id.getPosition();
  }

  public String getTextValue() { return textValue; }
  public void setTextValue(String textValue) { this.textValue = textValue; }

  public BigDecimal getNumberValue() { return numberValue; }
  public void setNumberValue(BigDecimal numberValue) { this.numberValue = numberValue; }

  public LocalDate getDateValue() { return dateValue; }
  public void setDateValue(LocalDate dateValue) { this.dateValue = dateValue; }

  public Option getOption() { return option; }
  public void setOption(Option option) { this.option = option; }
}
