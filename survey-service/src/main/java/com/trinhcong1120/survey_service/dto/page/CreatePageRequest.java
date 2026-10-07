package com.trinhcong1120.survey_service.dto.page;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePageRequest {

    @NotNull(message = "ID khảo sát không được để trống")
    private UUID surveyId;

    @NotBlank(message = "Tiêu đề trang không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Thứ tự trang không được để trống")
    private Integer orderIndex;

    public CreatePageRequest() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
