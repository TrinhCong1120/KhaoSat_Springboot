package com.trinhcong1120.survey_service.dto.survey;

import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SurveyDetailResponse {

    private Integer id;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String audioUrl;
    private String creatorUser;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Long validationRevision;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    private List<PageDetailResponse> pages = new ArrayList<>();
    private List<ConditionResponse> conditions = new ArrayList<>();

    public SurveyDetailResponse() {
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

    public Long getValidationRevision() {
        return validationRevision;
    }

    public void setValidationRevision(Long validationRevision) {
        this.validationRevision = validationRevision;
    }

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }

    public List<PageDetailResponse> getPages() {
        return pages;
    }

    public void setPages(List<PageDetailResponse> pages) {
        this.pages = pages;
    }

    public List<ConditionResponse> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionResponse> conditions) {
        this.conditions = conditions;
    }

    public static class PageDetailResponse {

        private Integer id;
        private String title;
        private String description;
        private String imageUrl;
        private String videoUrl;
        private String audioUrl;
        private Integer orderIndex;
        private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

        private List<QuestionResponse> questions = new ArrayList<>();

        public PageDetailResponse() {
        }

        public PageDetailResponse(
                Integer id,
                String title,
                Integer orderIndex,
                List<QuestionResponse> questions
        ) {
            this(id, title, null, null, null, null, orderIndex, questions);
        }

        public PageDetailResponse(
                Integer id,
                String title,
                String description,
                String imageUrl,
                String videoUrl,
                String audioUrl,
                Integer orderIndex,
                List<QuestionResponse> questions
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.videoUrl = videoUrl;
            this.audioUrl = audioUrl;
            this.orderIndex = orderIndex;
            this.questions = questions;
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

        public Integer getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }

        public List<QuestionResponse> getQuestions() {
            return questions;
        }

        public void setQuestions(List<QuestionResponse> questions) {
            this.questions = questions;
        }

        public List<MediaUploadResponse> getMediaFiles() {
            return mediaFiles;
        }

        public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
            this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
        }
    }
}
