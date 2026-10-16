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

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }
}
