package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_types")
public class QuestionType {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "code")
  private String code;

  @Column(name = "name")
  private String name;

  @JsonIgnore
  @OneToMany(mappedBy = "questionType", fetch = FetchType.LAZY)
  private List<Question> questions = new ArrayList<>();

  public QuestionType() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public List<Question> getQuestions() { return questions; }
  public void setQuestions(List<Question> questions) { this.questions = questions; }
}
