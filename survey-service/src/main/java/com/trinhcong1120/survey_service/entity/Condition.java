package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "conditions")
public class Condition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_question_id", nullable = false)
  private Question sourceQuestion;

  @Column(name = "source_value", nullable = false, columnDefinition = "TEXT")
  private String sourceValue;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_question_id", nullable = false)
  private Question targetQuestion;

  @Column(name = "action", nullable = false)
  private String action;

  public Condition() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Question getSourceQuestion() { return sourceQuestion; }
  public void setSourceQuestion(Question sourceQuestion) {
    this.sourceQuestion = sourceQuestion;
  }

  public String getSourceValue() { return sourceValue; }
  public void setSourceValue(String sourceValue) { this.sourceValue = sourceValue; }

  public Question getTargetQuestion() { return targetQuestion; }
  public void setTargetQuestion(Question targetQuestion) {
    this.targetQuestion = targetQuestion;
  }

  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
}