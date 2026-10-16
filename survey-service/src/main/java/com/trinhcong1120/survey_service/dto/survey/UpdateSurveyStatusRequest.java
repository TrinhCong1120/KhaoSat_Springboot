package com.trinhcong1120.survey_service.dto.survey;

import jakarta.validation.constraints.NotBlank;

public class UpdateSurveyStatusRequest {

    @NotBlank(message = "Hành động trạng thái không được để trống")
    private String action;

    public UpdateSurveyStatusRequest() {
    }

    public UpdateSurveyStatusRequest(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
