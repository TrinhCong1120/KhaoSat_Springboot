package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "responses")
public class Response {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_id")
  private Survey survey;

  @Column(name = "token", length = 500)
  private String token;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @JsonIgnore
  @OneToMany(
          mappedBy = "response",
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL
  )
  private List<Answer> answers = new ArrayList<>();

  public Response() {}

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Survey getSurvey() { return survey; }
  public void setSurvey(Survey survey) { this.survey = survey; }

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }

  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public UUID getRequestId() { return requestId; }
  public void setRequestId(UUID requestId) { this.requestId = requestId; }

  public List<Answer> getAnswers() { return answers; }
  public void setAnswers(List<Answer> answers) { this.answers = answers; }
}