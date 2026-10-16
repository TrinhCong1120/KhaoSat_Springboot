package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "survey_access", uniqueConstraints =
    @UniqueConstraint(columnNames = {"survey_id", "user_id"}))
public class SurveyAccess {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "survey_id", nullable = false)
  private Survey survey;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "can_view", nullable = false)
  private boolean canView;

  @Column(name = "can_edit", nullable = false)
  private boolean canEdit;

  public UUID getId() { return id; }
  public Survey getSurvey() { return survey; }
  public void setSurvey(Survey survey) { this.survey = survey; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public boolean isCanView() { return canView; }
  public void setCanView(boolean canView) { this.canView = canView; }
  public boolean isCanEdit() { return canEdit; }
  public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }
}
