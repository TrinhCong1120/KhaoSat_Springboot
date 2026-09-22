package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "answer_options")
public class AnswerOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_id")
  private Answer answer;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "option_id")
  private Option option;

  public AnswerOption() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Answer getAnswer() { return answer; }
  public void setAnswer(Answer answer) { this.answer = answer; }

  public Option getOption() { return option; }
  public void setOption(Option option) { this.option = option; }
}