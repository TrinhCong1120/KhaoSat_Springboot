package com.trinhcong1120.survey_service.dto.page;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePageRequest {

    @NotNull(message = "ID khảo sát không được để trống")
    private Integer surveyId;

    @NotBlank(message = "Tiêu đề trang không được để trống")
    private String title;

    @NotNull(message = "Thứ tự trang không được để trống")
    private Integer orderIndex;

    public CreatePageRequest() {
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
