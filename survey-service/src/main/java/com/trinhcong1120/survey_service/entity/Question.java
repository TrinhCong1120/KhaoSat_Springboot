package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "page_id")
  private Page page;

  @Column(name = "question_text", columnDefinition = "TEXT")
  private String questionText;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_type_id")
  private QuestionType questionType;

  @Column(name = "is_required")
  private Boolean isRequired;

  @Column(name = "order_index")
  private Integer orderIndex;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @JsonIgnore
  @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
  private List<Option> options = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
  private List<Answer> answers = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "sourceQuestion", fetch = FetchType.LAZY)
  private List<Condition> sourceConditions = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "targetQuestion", fetch = FetchType.LAZY)
  private List<Condition> targetConditions = new ArrayList<>();

  public Question() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Page getPage() { return page; }
  public void setPage(Page page) { this.page = page; }

  public String getQuestionText() { return questionText; }
  public void setQuestionText(String questionText) { this.questionText = questionText; }

  public QuestionType getQuestionType() { return questionType; }
  public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

  public Boolean getIsRequired() { return isRequired; }
  public void setIsRequired(Boolean required) { isRequired = required; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public List<Option> getOptions() { return options; }
  public void setOptions(List<Option> options) { this.options = options; }

  public List<Answer> getAnswers() { return answers; }
  public void setAnswers(List<Answer> answers) { this.answers = answers; }

  public List<Condition> getSourceConditions() { return sourceConditions; }
  public void setSourceConditions(List<Condition> sourceConditions) {
    this.sourceConditions = sourceConditions;
  }

  public List<Condition> getTargetConditions() { return targetConditions; }
  public void setTargetConditions(List<Condition> targetConditions) {
    this.targetConditions = targetConditions;
  }
}