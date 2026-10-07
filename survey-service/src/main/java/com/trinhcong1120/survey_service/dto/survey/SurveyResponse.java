package com.trinhcong1120.survey_service.dto.survey;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SurveyResponse {

    private UUID id;
    private String title;
    private String description;
    private String creatorUser;
    private UUID creatorUserId;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public SurveyResponse() {
    }

    public SurveyResponse(
            UUID id,
            String title,
            String description,
            String creatorUser,
            LocalDateTime createdAt,
            Boolean isActive
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorUser = creatorUser;
        this.createdAt = createdAt;
        this.isActive = isActive;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }
}
