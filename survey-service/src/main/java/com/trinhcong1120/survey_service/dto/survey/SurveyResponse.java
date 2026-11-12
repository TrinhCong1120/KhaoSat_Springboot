package com.trinhcong1120.survey_service.dto.survey;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SurveyResponse {

    private UUID id;
    private String title;
    private String shareCode;
    private String description;
    private String creatorUser;
    private UUID creatorUserId;
    private UUID groupId;
    private boolean canViewResponses;
    private boolean canExportData;
    private boolean canManagePublication;
    private boolean sharedDirectly;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canManageAccess;
    private LocalDateTime createdAt;
    private String status;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public SurveyResponse() {
    }

    public SurveyResponse(
            UUID id,
            String title,
            String description,
            String creatorUser,
            LocalDateTime createdAt,
            String status
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorUser = creatorUser;
        this.createdAt = createdAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorUser() {
        return creatorUser;
    }

    public UUID getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(UUID creatorUserId) { this.creatorUserId = creatorUserId; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public boolean isCanViewResponses() { return canViewResponses; }
    public void setCanViewResponses(boolean value) { canViewResponses = value; }
    public boolean isCanExportData() { return canExportData; }
    public void setCanExportData(boolean value) { canExportData = value; }
    public boolean isCanManagePublication() { return canManagePublication; }
    public void setCanManagePublication(boolean value) { canManagePublication = value; }
    public boolean isSharedDirectly() { return sharedDirectly; }
    public void setSharedDirectly(boolean value) { sharedDirectly = value; }
    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }
    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
    public boolean isCanManageAccess() { return canManageAccess; }
    public void setCanManageAccess(boolean canManageAccess) { this.canManageAccess = canManageAccess; }

    public void setCreatorUser(String creatorUser) {
        this.creatorUser = creatorUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; }

    private GoogleIntegrationDto googleIntegration;

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }

    public GoogleIntegrationDto getGoogleIntegration() { return googleIntegration; }
    public void setGoogleIntegration(GoogleIntegrationDto googleIntegration) { this.googleIntegration = googleIntegration; }
}

