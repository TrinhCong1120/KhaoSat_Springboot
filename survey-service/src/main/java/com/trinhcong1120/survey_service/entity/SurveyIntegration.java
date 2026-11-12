package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "survey_integrations")
public class SurveyIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "survey_id", nullable = false, unique = true)
    private UUID surveyId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider = "GOOGLE_FORMS";

    @Column(name = "google_form_id", nullable = false, length = 255)
    private String googleFormId;

    @Column(name = "owner_connection_id")
    private UUID ownerConnectionId;

    @Column(name = "edit_url", nullable = false, columnDefinition = "TEXT")
    private String editUrl;

    @Column(name = "response_url", nullable = false, columnDefinition = "TEXT")
    private String responseUrl;

    @Column(name = "spreadsheet_id", length = 255)
    private String spreadsheetId;

    @Column(name = "sync_status", nullable = false, length = 32)
    private String syncStatus = "IDLE";

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_sync_error", columnDefinition = "TEXT")
    private String lastSyncError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SurveyIntegration() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSurveyId() { return surveyId; }
    public void setSurveyId(UUID surveyId) { this.surveyId = surveyId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getGoogleFormId() { return googleFormId; }
    public void setGoogleFormId(String googleFormId) { this.googleFormId = googleFormId; }

    public UUID getOwnerConnectionId() { return ownerConnectionId; }
    public void setOwnerConnectionId(UUID ownerConnectionId) { this.ownerConnectionId = ownerConnectionId; }

    public String getEditUrl() { return editUrl; }
    public void setEditUrl(String editUrl) { this.editUrl = editUrl; }

    public String getResponseUrl() { return responseUrl; }
    public void setResponseUrl(String responseUrl) { this.responseUrl = responseUrl; }

    public String getSpreadsheetId() { return spreadsheetId; }
    public void setSpreadsheetId(String spreadsheetId) { this.spreadsheetId = spreadsheetId; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
