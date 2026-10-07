package com.trinhcong1120.survey_service.dto.dashboard;

import java.util.UUID;

public class TopSurveyResponse {

    private UUID surveyId;
    private String title;
    private Double completionPercent;
    private Long responseCount;

    public TopSurveyResponse() {
    }

    public TopSurveyResponse(
            UUID surveyId,
            String title,
            Double completionPercent,
            Long responseCount
    ) {
        this.surveyId = surveyId;
        this.title = title;
        this.completionPercent = completionPercent;
        this.responseCount = responseCount;
    }

    public UUID getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(UUID surveyId) {
        this.surveyId = surveyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getCompletionPercent() {
        return completionPercent;
    }

    public void setCompletionPercent(
            Double completionPercent
    ) {
        this.completionPercent = completionPercent;
    }

    public Long getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
    }
}