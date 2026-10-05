package com.trinhcong1120.survey_service.dto.survey;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SurveyResponse {

    private Integer id;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String audioUrl;
    private String creatorUser;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public SurveyResponse() {
    }

    public SurveyResponse(
            Integer id,
            String title,
            String description,
            String creatorUser,
            LocalDateTime createdAt,
            Boolean isActive
    ) {
        this(id, title, description, null, null, null, creatorUser, createdAt, isActive);
    }

    public SurveyResponse(
            Integer id,
            String title,
            String description,
            String imageUrl,
            String videoUrl,
            String audioUrl,
            String creatorUser,
            LocalDateTime createdAt,
            Boolean isActive
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.creatorUser = creatorUser;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getCreatorUser() {
        return creatorUser;
    }

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
