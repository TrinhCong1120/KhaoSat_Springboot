package com.trinhcong1120.survey_service.dto.survey;

import java.time.Instant;
import java.util.UUID;

public class GoogleIntegrationDto {
    private UUID id;
    private String provider = "GOOGLE_FORMS";
    private String googleFormId;
    private String editUrl;
    private String responseUrl;
    private String syncStatus;
    private Instant lastSyncedAt;
    private String lastSyncError;
    private Boolean isAcceptingResponses;

    public GoogleIntegrationDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getGoogleFormId() { return googleFormId; }
    public void setGoogleFormId(String googleFormId) { this.googleFormId = googleFormId; }

    public String getEditUrl() { return editUrl; }
    public void setEditUrl(String editUrl) { this.editUrl = editUrl; }

    public String getResponseUrl() { return responseUrl; }
    public void setResponseUrl(String responseUrl) { this.responseUrl = responseUrl; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

    public Boolean getIsAcceptingResponses() { return isAcceptingResponses; }
    public void setIsAcceptingResponses(Boolean isAcceptingResponses) { this.isAcceptingResponses = isAcceptingResponses; }
}
