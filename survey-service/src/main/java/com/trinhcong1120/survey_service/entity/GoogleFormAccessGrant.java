package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_form_access_grants")
public class GoogleFormAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "survey_integration_id", nullable = false)
    private UUID surveyIntegrationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "google_email", nullable = false, length = 255)
    private String googleEmail;

    @Column(name = "google_permission_id", length = 255)
    private String googlePermissionId;

    @Column(name = "role", nullable = false, length = 32)
    private String role = "writer";

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public GoogleFormAccessGrant() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSurveyIntegrationId() { return surveyIntegrationId; }
    public void setSurveyIntegrationId(UUID surveyIntegrationId) { this.surveyIntegrationId = surveyIntegrationId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getGoogleEmail() { return googleEmail; }
    public void setGoogleEmail(String googleEmail) { this.googleEmail = googleEmail; }

    public String getGooglePermissionId() { return googlePermissionId; }
    public void setGooglePermissionId(String googlePermissionId) { this.googlePermissionId = googlePermissionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(UUID grantedByUserId) { this.grantedByUserId = grantedByUserId; }

    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
