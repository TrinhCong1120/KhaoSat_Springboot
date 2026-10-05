package com.trinhcong1120.survey_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ResponseDetailResponse {

    private Integer id;
    private Integer surveyId;
    private String surveyTitle;
    private String surveyDescription;
    private String surveyImageUrl;
    private String surveyVideoUrl;
    private String surveyAudioUrl;

    private UUID requestId;
    private LocalDateTime submittedAt;

    private List<ResponsePageResponse> pages;

    public ResponseDetailResponse() {
    }

    public ResponseDetailResponse(
            Integer id,
            Integer surveyId,
            String surveyTitle,
            UUID requestId,
            LocalDateTime submittedAt,
            List<ResponsePageResponse> pages
    ) {
        this(id, surveyId, surveyTitle, null, null, null, null, requestId, submittedAt, pages);
    }

    public ResponseDetailResponse(
            Integer id,
            Integer surveyId,
            String surveyTitle,
            String surveyDescription,
            String surveyImageUrl,
            String surveyVideoUrl,
            String surveyAudioUrl,
            UUID requestId,
            LocalDateTime submittedAt,
            List<ResponsePageResponse> pages
    ) {
        this.id = id;
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
        this.surveyDescription = surveyDescription;
        this.surveyImageUrl = surveyImageUrl;
        this.surveyVideoUrl = surveyVideoUrl;
        this.surveyAudioUrl = surveyAudioUrl;
        this.requestId = requestId;
        this.submittedAt = submittedAt;
        this.pages = pages;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Integer surveyId) {
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

    public String getSurveyImageUrl() {
        return surveyImageUrl;
    }

    public void setSurveyImageUrl(String surveyImageUrl) {
        this.surveyImageUrl = surveyImageUrl;
    }

    public String getSurveyVideoUrl() {
        return surveyVideoUrl;
    }

    public void setSurveyVideoUrl(String surveyVideoUrl) {
        this.surveyVideoUrl = surveyVideoUrl;
    }

    public String getSurveyAudioUrl() {
        return surveyAudioUrl;
    }

    public void setSurveyAudioUrl(String surveyAudioUrl) {
        this.surveyAudioUrl = surveyAudioUrl;
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
