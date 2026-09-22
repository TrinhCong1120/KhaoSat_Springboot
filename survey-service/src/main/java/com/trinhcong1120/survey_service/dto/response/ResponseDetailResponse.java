package com.trinhcong1120.survey_service.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ResponseDetailResponse {

    private Integer id;
    private Integer surveyId;
    private String surveyTitle;

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
        this.id = id;
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
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