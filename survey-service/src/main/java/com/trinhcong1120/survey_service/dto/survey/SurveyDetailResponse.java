package com.trinhcong1120.survey_service.dto.survey;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SurveyDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private String creatorUser;
    private UUID creatorUserId;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Long validationRevision;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    private List<PageDetailResponse> pages = new ArrayList<>();
    private List<ConditionResponse> conditions = new ArrayList<>();

    public SurveyDetailResponse() {
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

        private UUID id;
        private String title;
        private String description;
        private Integer orderIndex;
        private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

        private List<QuestionResponse> questions = new ArrayList<>();

        public PageDetailResponse() {
        }

        public PageDetailResponse(
                UUID id,
                String title,
                Integer orderIndex,
                List<QuestionResponse> questions
        ) {
            this(id, title, null, orderIndex, questions);
        }

        public PageDetailResponse(
                UUID id,
                String title,
                String description,
                Integer orderIndex,
                List<QuestionResponse> questions
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.orderIndex = orderIndex;
            this.questions = questions;
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
