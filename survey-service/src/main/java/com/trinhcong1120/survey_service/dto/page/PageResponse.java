package com.trinhcong1120.survey_service.dto.page;

public class PageResponse {

    private Integer id;
    private Integer surveyId;
    private String title;
    private Integer orderIndex;

    public PageResponse() {
    }

    public PageResponse(
            Integer id,
            Integer surveyId,
            String title,
            Integer orderIndex
    ) {
        this.id = id;
        this.surveyId = surveyId;
        this.title = title;
        this.orderIndex = orderIndex;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}