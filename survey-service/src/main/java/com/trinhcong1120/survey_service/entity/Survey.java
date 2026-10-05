package com.trinhcong1120.survey_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
public class Survey {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "title", length = 500)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "image_url", columnDefinition = "TEXT")
  private String imageUrl;

  @Column(name = "video_url", columnDefinition = "TEXT")
  private String videoUrl;

  @Column(name = "audio_url", columnDefinition = "TEXT")
  private String audioUrl;

  @Column(name = "creator_user")
  private String creatorUser;

  @Column(name = "creator_password", length = 500)
  private String creatorPassword;

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

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

  public String getVideoUrl() { return videoUrl; }
  public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

  public String getAudioUrl() { return audioUrl; }
  public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

  public String getCreatorUser() { return creatorUser; }
  public void setCreatorUser(String creatorUser) { this.creatorUser = creatorUser; }

  public String getCreatorPassword() { return creatorPassword; }
  public void setCreatorPassword(String creatorPassword) { this.creatorPassword = creatorPassword; }

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
