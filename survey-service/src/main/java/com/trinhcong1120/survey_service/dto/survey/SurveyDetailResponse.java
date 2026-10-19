package com.trinhcong1120.survey_service.dto.survey;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;
import com.trinhcong1120.survey_service.dto.validation.ValidationRuleCatalogResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SurveyDetailResponse {

    private UUID id;
    private String title;
    private String shareCode;
    private String description;
    private String creatorUser;
    private UUID creatorUserId;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canManageAccess;
    private LocalDateTime createdAt;
    private String status;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private LocalDateTime serverTime;
    private Long validationRevision;
    private List<ScheduleWarning> scheduleWarnings = new ArrayList<>();
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    private List<PageDetailResponse> pages = new ArrayList<>();
    private List<ConditionResponse> conditions = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ValidationRuleCatalogResponse validationRuleCatalog;

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
    public LocalDateTime getServerTime() { return serverTime; }
    public void setServerTime(LocalDateTime serverTime) { this.serverTime = serverTime; }
    public List<ScheduleWarning> getScheduleWarnings() { return scheduleWarnings; }
    public void setScheduleWarnings(List<ScheduleWarning> scheduleWarnings) { this.scheduleWarnings = scheduleWarnings; }

    public record ScheduleWarning(UUID conditionId, String message) {}

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

    public ValidationRuleCatalogResponse getValidationRuleCatalog() {
        return validationRuleCatalog;
    }

    public void setValidationRuleCatalog(ValidationRuleCatalogResponse validationRuleCatalog) {
        this.validationRuleCatalog = validationRuleCatalog;
    }

    public static class PageDetailResponse {

        private UUID id;
        private String title;
        private String description;
        private Integer orderIndex;
        private LocalDateTime openAt;
        private LocalDateTime closeAt;
        private LocalDateTime effectiveCloseAt;
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
        public LocalDateTime getOpenAt() { return openAt; }
        public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; }
        public LocalDateTime getCloseAt() { return closeAt; }
        public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; }
        public LocalDateTime getEffectiveCloseAt() { return effectiveCloseAt; }
        public void setEffectiveCloseAt(LocalDateTime effectiveCloseAt) { this.effectiveCloseAt = effectiveCloseAt; }

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
