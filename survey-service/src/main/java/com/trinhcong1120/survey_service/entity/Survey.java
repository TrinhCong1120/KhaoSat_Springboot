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

  @Column(name = "creator_user")
  private String creatorUser;

  @Column(name = "creator_password", length = 500)
  private String creatorPassword;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "is_active")
  private Boolean isActive;

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

  public String getCreatorUser() { return creatorUser; }
  public void setCreatorUser(String creatorUser) { this.creatorUser = creatorUser; }

  public String getCreatorPassword() { return creatorPassword; }
  public void setCreatorPassword(String creatorPassword) { this.creatorPassword = creatorPassword; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }

  public List<Page> getPages() { return pages; }
  public void setPages(List<Page> pages) { this.pages = pages; }

  public List<Response> getResponses() { return responses; }
  public void setResponses(List<Response> responses) { this.responses = responses; }
}