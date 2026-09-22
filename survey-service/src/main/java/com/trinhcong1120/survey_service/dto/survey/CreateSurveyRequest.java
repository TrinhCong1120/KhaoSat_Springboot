package com.trinhcong1120.survey_service.dto.survey;

import jakarta.validation.constraints.NotBlank;

public class CreateSurveyRequest {

    @NotBlank(message = "Tiêu đề khảo sát không được để trống")
    private String title;

    private String description;

    public CreateSurveyRequest() {
    }

    public CreateSurveyRequest(String title, String description) {
        this.title = title;
        this.description = description;
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
}
