package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

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
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Question parent;

  @JsonIgnore
  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
  private List<Question> children = new ArrayList<>();

  @Column(name = "level")
  private Integer level = 1;

  @Column(name = "path", columnDefinition = "TEXT")
  private String path = "";

  @Column(name = "is_leaf")
  private Boolean isLeaf = true;

  @Column(name = "aggregation_type", length = 50)
  private String aggregationType = "NONE";

  @Column(name = "aggregation_formula", columnDefinition = "TEXT")
  private String aggregationFormula;

  @Column(name = "weight")
  private java.math.BigDecimal weight = java.math.BigDecimal.ONE;

  @Column(name = "matrix_config", columnDefinition = "TEXT")
  private String matrixConfig;

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

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

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

  public Question getParent() { return parent; }
  public void setParent(Question parent) { this.parent = parent; }

  public List<Question> getChildren() { return children; }
  public void setChildren(List<Question> children) { this.children = children; }

  public Integer getLevel() { return level != null ? level : 1; }
  public void setLevel(Integer level) { this.level = level; }

  public String getPath() { return path != null ? path : ""; }
  public void setPath(String path) { this.path = path; }

  public Boolean getIsLeaf() { return isLeaf != null ? isLeaf : true; }
  public void setIsLeaf(Boolean isLeaf) { this.isLeaf = isLeaf; }

  public String getAggregationType() { return aggregationType != null ? aggregationType : "NONE"; }
  public void setAggregationType(String aggregationType) { this.aggregationType = aggregationType; }

  public String getAggregationFormula() { return aggregationFormula; }
  public void setAggregationFormula(String aggregationFormula) { this.aggregationFormula = aggregationFormula; }

  public java.math.BigDecimal getWeight() { return weight != null ? weight : java.math.BigDecimal.ONE; }
  public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }

  public String getMatrixConfig() { return matrixConfig; }
  public void setMatrixConfig(String matrixConfig) { this.matrixConfig = matrixConfig; }
}
