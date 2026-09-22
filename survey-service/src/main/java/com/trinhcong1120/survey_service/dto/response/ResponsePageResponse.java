package com.trinhcong1120.survey_service.dto.response;

import java.util.List;

public class ResponsePageResponse {

    private Integer pageId;
    private String title;
    private Integer orderIndex;

    private List<ResponseQuestionResponse> questions;

    public ResponsePageResponse() {
    }

    public ResponsePageResponse(
            Integer pageId,
            String title,
            Integer orderIndex,
            List<ResponseQuestionResponse> questions
    ) {
        this.pageId = pageId;
        this.title = title;
        this.orderIndex = orderIndex;
        this.questions = questions;
    }

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
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

    public List<ResponseQuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ResponseQuestionResponse> questions) {
        this.questions = questions;
    }
}