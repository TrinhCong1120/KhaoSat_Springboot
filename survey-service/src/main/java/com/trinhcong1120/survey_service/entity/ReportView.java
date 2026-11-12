package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "report_views")
public class ReportView {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "survey_id", nullable = false)
  private UUID surveyId;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "scope", nullable = false, length = 20)
  private String scope = "PRIVATE";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "definition_json", nullable = false, columnDefinition = "jsonb")
  private String definitionJson;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getSurveyId() { return surveyId; }
  public void setSurveyId(UUID surveyId) { this.surveyId = surveyId; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }

  public String getDefinitionJson() { return definitionJson; }
  public void setDefinitionJson(String definitionJson) { this.definitionJson = definitionJson; }

  public UUID getCreatedByUserId() { return createdByUserId; }
  public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

  public boolean isDefault() { return isDefault; }
  public void setDefault(boolean aDefault) { isDefault = aDefault; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

