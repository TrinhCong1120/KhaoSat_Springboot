package com.trinhcong1120.survey_service.entity;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.Instant;
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

  @Column(name = "share_code", length = 24, nullable = false, unique = true)
  private String shareCode;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "creator_user")
  private String creatorUser;

  @Column(name = "creator_user_id")
  private UUID creatorUserId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "open_at")
  private Instant openAt;

  @Column(name = "close_at")
  private Instant closeAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "manually_locked", nullable = false)
  private boolean manuallyLocked;

  @Column(name = "requires_unlock", nullable = false)
  private boolean requiresUnlock;

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

  public String getShareCode() { return shareCode; }
  public void setShareCode(String shareCode) { this.shareCode = shareCode; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getCreatorUser() { return creatorUser; }
  public void setCreatorUser(String creatorUser) { this.creatorUser = creatorUser; }
  public UUID getCreatorUserId() { return creatorUserId; }
  public void setCreatorUserId(UUID creatorUserId) { this.creatorUserId = creatorUserId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public Instant getOpenAt() { return openAt; }
  public void setOpenAt(Instant openAt) { this.openAt = openAt; }
  public Instant getCloseAt() { return closeAt; }
  public void setCloseAt(Instant closeAt) { this.closeAt = closeAt; }
  public Instant getPublishedAt() { return publishedAt; }
  public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
  public boolean isManuallyLocked() { return manuallyLocked; }
  public void setManuallyLocked(boolean manuallyLocked) { this.manuallyLocked = manuallyLocked; }
  public boolean isRequiresUnlock() { return requiresUnlock; }
  public void setRequiresUnlock(boolean requiresUnlock) { this.requiresUnlock = requiresUnlock; }

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
