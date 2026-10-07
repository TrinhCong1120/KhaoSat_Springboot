package com.trinhcong1120.survey_service.dto.response;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResponseDetailResponse {

    private UUID id;
    private UUID surveyId;
    private String surveyTitle;
    private String surveyDescription;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    private UUID requestId;
    private LocalDateTime submittedAt;

    private List<ResponsePageResponse> pages;

    public ResponseDetailResponse() {
    }

    public ResponseDetailResponse(
            UUID id,
            UUID surveyId,
            String surveyTitle,
            UUID requestId,
            LocalDateTime submittedAt,
            List<ResponsePageResponse> pages
    ) {
        this(id, surveyId, surveyTitle, null, requestId, submittedAt, pages);
    }

    public ResponseDetailResponse(
            UUID id,
            UUID surveyId,
            String surveyTitle,
            String surveyDescription,
            UUID requestId,
            LocalDateTime submittedAt,
            List<ResponsePageResponse> pages
    ) {
        this.id = id;
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
        this.surveyDescription = surveyDescription;
        this.requestId = requestId;
        this.submittedAt = submittedAt;
        this.pages = pages;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(UUID surveyId) {
        this.surveyId = surveyId;
    }

    public String getSurveyTitle() {
        return surveyTitle;
    }

    public void setSurveyTitle(String surveyTitle) {
        this.surveyTitle = surveyTitle;
    }

    public String getSurveyDescription() {
        return surveyDescription;
    }

    public void setSurveyDescription(String surveyDescription) {
        this.surveyDescription = surveyDescription;
    }

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<ResponsePageResponse> getPages() {
        return pages;
    }

    public void setPages(List<ResponsePageResponse> pages) {
        this.pages = pages;
    }
}
