package com.trinhcong1120.survey_service.dto.statistics;

import java.util.List;

public class SurveyStatisticsResponse {

    private Integer surveyId;
    private String surveyTitle;
    private Long totalResponses;
    private List<QuestionStatisticsResponse> questions;

    public SurveyStatisticsResponse() {
    }

    public SurveyStatisticsResponse(
            Integer surveyId,
            String surveyTitle,
            Long totalResponses,
            List<QuestionStatisticsResponse> questions
    ) {
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
        this.totalResponses = totalResponses;
        this.questions = questions;
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