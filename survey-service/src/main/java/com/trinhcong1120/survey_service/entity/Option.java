package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "options")
public class Option {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id")
  private Question question;

  @Column(name = "option_text", columnDefinition = "TEXT")
  private String optionText;

  @Column(name = "order_index")
  private Integer orderIndex;

  @JsonIgnore
  @OneToMany(mappedBy = "option", fetch = FetchType.LAZY)
  private List<AnswerOption> answerOptions = new ArrayList<>();

  public Option() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Question getQuestion() { return question; }
  public void setQuestion(Question question) { this.question = question; }

  public String getOptionText() { return optionText; }
  public void setOptionText(String optionText) { this.optionText = optionText; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

  public List<AnswerOption> getAnswerOptions() { return answerOptions; }
  public void setAnswerOptions(List<AnswerOption> answerOptions) {
    this.answerOptions = answerOptions;
  }
}