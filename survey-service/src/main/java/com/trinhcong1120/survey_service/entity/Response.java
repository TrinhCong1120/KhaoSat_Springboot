package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "responses")
public class Response {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_id")
  private Survey survey;

  @Column(name = "token", length = 500)
  private String token;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "request_id", nullable = false)
  private UUID requestId;

  @Column(name = "request_hash", length = 64)
  private String requestHash;

  @Column(name = "session_id")
  private UUID sessionId;

  @Column(name = "applicable_question_count")
  private Integer applicableQuestionCount;

  @Column(name = "answered_question_count")
  private Integer answeredQuestionCount;

  @JsonIgnore
  @OneToMany(
          mappedBy = "response",
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL
  )
  private List<Answer> answers = new ArrayList<>();

  public Response() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Survey getSurvey() { return survey; }
  public void setSurvey(Survey survey) { this.survey = survey; }

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }

  // Legacy DTOs expose a timezone-less UTC value. New analytics uses the Instant directly.
  public LocalDateTime getSubmittedAt() {
    return submittedAt == null ? null : LocalDateTime.ofInstant(submittedAt, ZoneOffset.UTC);
  }
  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt == null ? null : submittedAt.toInstant(ZoneOffset.UTC);
  }
  @JsonIgnore
  public Instant getSubmittedAtInstant() { return submittedAt; }
  public void setSubmittedAtInstant(Instant submittedAt) { this.submittedAt = submittedAt; }

  public UUID getRequestId() { return requestId; }
  public void setRequestId(UUID requestId) { this.requestId = requestId; }
  public String getRequestHash() { return requestHash; }
  public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
  public UUID getSessionId() { return sessionId; }
  public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
  public Integer getApplicableQuestionCount() { return applicableQuestionCount; }
  public void setApplicableQuestionCount(Integer value) { this.applicableQuestionCount = value; }
  public Integer getAnsweredQuestionCount() { return answeredQuestionCount; }
  public void setAnsweredQuestionCount(Integer value) { this.answeredQuestionCount = value; }

  public List<Answer> getAnswers() { return answers; }
  public void setAnswers(List<Answer> answers) { this.answers = answers; }
}
