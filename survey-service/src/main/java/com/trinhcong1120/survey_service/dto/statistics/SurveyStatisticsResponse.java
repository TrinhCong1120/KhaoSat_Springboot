package com.trinhcong1120.survey_service.dto.statistics;

import java.util.UUID;

import java.util.List;

public class SurveyStatisticsResponse {

    private UUID surveyId;
    private String surveyTitle;
    private Long totalResponses;
    private List<QuestionStatisticsResponse> questions;

    public SurveyStatisticsResponse() {
    }

    public SurveyStatisticsResponse(
            UUID surveyId,
            String surveyTitle,
            Long totalResponses,
            List<QuestionStatisticsResponse> questions
    ) {
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
        this.totalResponses = totalResponses;
        this.questions = questions;
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

    public Long getTotalResponses() {
        return totalResponses;
    }

    public void setTotalResponses(Long totalResponses) {
        this.totalResponses = totalResponses;
    }

    public List<QuestionStatisticsResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionStatisticsResponse> questions) {
        this.questions = questions;
    }
}