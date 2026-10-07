package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
public class Survey {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "title", length = 500)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "creator_user")
  private String creatorUser;

  @Column(name = "creator_user_id")
  private UUID creatorUserId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "validation_revision", nullable = false)
  private Long validationRevision = 1L;

  @JsonIgnore
  @OneToMany(mappedBy = "survey", fetch = FetchType.LAZY)
  private List<Page> pages = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "survey", fetch = FetchType.LAZY)
  private List<Response> responses = new ArrayList<>();

  public Survey() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getCreatorUser() { return creatorUser; }
  public void setCreatorUser(String creatorUser) { this.creatorUser = creatorUser; }
  public UUID getCreatorUserId() { return creatorUserId; }
  public void setCreatorUserId(UUID creatorUserId) { this.creatorUserId = creatorUserId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }

  public Long getValidationRevision() { return validationRevision; }
  public void setValidationRevision(Long validationRevision) {
    this.validationRevision = validationRevision == null ? 1L : validationRevision;
  }

  public void incrementValidationRevision() {
    this.validationRevision = this.validationRevision == null
            ? 1L
            : this.validationRevision + 1;
  }

  public List<Page> getPages() { return pages; }
  public void setPages(List<Page> pages) { this.pages = pages; }

  public List<Response> getResponses() { return responses; }
  public void setResponses(List<Response> responses) { this.responses = responses; }
}
