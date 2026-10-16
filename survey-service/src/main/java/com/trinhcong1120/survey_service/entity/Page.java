package com.trinhcong1120.survey_service.entity;

import java.util.UUID;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pages")
public class Page {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "survey_id")
  private Survey survey;

  @Column(name = "title", length = 500)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "order_index")
  private Integer orderIndex;

  @Column(name = "open_at")
  private Instant openAt;

  @Column(name = "close_at")
  private Instant closeAt;

  @JsonIgnore
  @OneToMany(mappedBy = "page", fetch = FetchType.LAZY)
  private List<Question> questions = new ArrayList<>();

  public Page() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Survey getSurvey() { return survey; }
  public void setSurvey(Survey survey) { this.survey = survey; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public Integer getOrderIndex() { return orderIndex; }
  public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
  public Instant getOpenAt() { return openAt; }
  public void setOpenAt(Instant openAt) { this.openAt = openAt; }
  public Instant getCloseAt() { return closeAt; }
  public void setCloseAt(Instant closeAt) { this.closeAt = closeAt; }

  public List<Question> getQuestions() { return questions; }
  public void setQuestions(List<Question> questions) { this.questions = questions; }
}
