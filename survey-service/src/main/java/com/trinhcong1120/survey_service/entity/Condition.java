package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "conditions")
public class Condition {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_question_id", nullable = false)
  private Question sourceQuestion;

  @Column(name = "source_value", nullable = false, columnDefinition = "TEXT")
  private String sourceValue;

  @Column(name = "target_type", length = 50)
  private String targetType = "QUESTION";

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_question_id")
  private Question targetQuestion;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_page_id")
  private Page targetPage;

  @Column(name = "action", nullable = false)
  private String action;

  public Condition() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Question getSourceQuestion() { return sourceQuestion; }
  public void setSourceQuestion(Question sourceQuestion) {
    this.sourceQuestion = sourceQuestion;
  }

  public String getSourceValue() { return sourceValue; }
  public void setSourceValue(String sourceValue) { this.sourceValue = sourceValue; }

  public String getTargetType() { return targetType != null ? targetType : "QUESTION"; }
  public void setTargetType(String targetType) { this.targetType = targetType; }

  public Question getTargetQuestion() { return targetQuestion; }
  public void setTargetQuestion(Question targetQuestion) {
    this.targetQuestion = targetQuestion;
  }

  public Page getTargetPage() { return targetPage; }
  public void setTargetPage(Page targetPage) {
    this.targetPage = targetPage;
  }

  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
}