package com.trinhcong1120.survey_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class ResponseListResponse {

    private Integer id;
    private Integer surveyId;
    private UUID requestId;
    private LocalDateTime submittedAt;
    private String preview;

    public ResponseListResponse() {
    }

    public ResponseListResponse(
            Integer id,
            Integer surveyId,
            UUID requestId,
            LocalDateTime submittedAt,
            String preview
    ) {
        this.id = id;
        this.surveyId = surveyId;
        this.requestId = requestId;
        this.submittedAt = submittedAt;
        this.preview = preview;
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

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}