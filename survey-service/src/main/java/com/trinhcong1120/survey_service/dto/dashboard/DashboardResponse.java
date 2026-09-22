package com.trinhcong1120.survey_service.dto.dashboard;

import java.util.List;

public class DashboardResponse {

    private Long openSurveys;
    private Double openSurveysDelta;

    private Long totalResponses;
    private Long responsesLast30Days;
    private Double responsesTrendPercent;

    private Double completionRate;

    private Double satisfactionScore;
    private Double satisfactionTrendDelta;

    private List<ActivityResponse> activities;
    private List<SurveyStatusResponse> surveyStatuses;
    private List<TopSurveyResponse> topSurveys;

    public DashboardResponse() {
    }

    public DashboardResponse(
            Long openSurveys,
            Double openSurveysDelta,
            Long totalResponses,
            Long responsesLast30Days,
            Double responsesTrendPercent,
            Double completionRate,
            Double satisfactionScore,
            Double satisfactionTrendDelta,
            List<ActivityResponse> activities,
            List<SurveyStatusResponse> surveyStatuses,
            List<TopSurveyResponse> topSurveys
    ) {
        this.openSurveys = openSurveys;
        this.openSurveysDelta = openSurveysDelta;
        this.totalResponses = totalResponses;
        this.responsesLast30Days = responsesLast30Days;
        this.responsesTrendPercent = responsesTrendPercent;
        this.completionRate = completionRate;
        this.satisfactionScore = satisfactionScore;
        this.satisfactionTrendDelta = satisfactionTrendDelta;
        this.activities = activities;
        this.surveyStatuses = surveyStatuses;
        this.topSurveys = topSurveys;
    }

    public Long getOpenSurveys() {
        return openSurveys;
    }

    public void setOpenSurveys(Long openSurveys) {
        this.openSurveys = openSurveys;
    }

    public Double getOpenSurveysDelta() {
        return openSurveysDelta;
    }

    public void setOpenSurveysDelta(Double openSurveysDelta) {
        this.openSurveysDelta = openSurveysDelta;
    }

    public Long getTotalResponses() {
        return totalResponses;
    }

    public void setTotalResponses(Long totalResponses) {
        this.totalResponses = totalResponses;
    }

    public Long getResponsesLast30Days() {
        return responsesLast30Days;
    }

    public void setResponsesLast30Days(Long responsesLast30Days) {
        this.responsesLast30Days = responsesLast30Days;
    }

    public Double getResponsesTrendPercent() {
        return responsesTrendPercent;
    }

    public void setResponsesTrendPercent(Double responsesTrendPercent) {
        this.responsesTrendPercent = responsesTrendPercent;
    }

    public Double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }

    public Double getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(Double satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public Double getSatisfactionTrendDelta() {
        return satisfactionTrendDelta;
    }

    public void setSatisfactionTrendDelta(
            Double satisfactionTrendDelta
    ) {
        this.satisfactionTrendDelta = satisfactionTrendDelta;
    }

    public List<ActivityResponse> getActivities() {
        return activities;
    }

    public void setActivities(
            List<ActivityResponse> activities
    ) {
        this.activities = activities;
    }

    public List<SurveyStatusResponse> getSurveyStatuses() {
        return surveyStatuses;
    }

    public void setSurveyStatuses(
            List<SurveyStatusResponse> surveyStatuses
    ) {
        this.surveyStatuses = surveyStatuses;
    }

    public List<TopSurveyResponse> getTopSurveys() {
        return topSurveys;
    }

    public void setTopSurveys(
            List<TopSurveyResponse> topSurveys
    ) {
        this.topSurveys = topSurveys;
    }
}