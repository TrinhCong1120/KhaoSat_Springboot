package com.trinhcong1120.survey_service.dto.survey;

import jakarta.validation.constraints.NotNull;

public class UpdateSurveyStatusRequest {

    @NotNull(message = "Trạng thái khảo sát không được để trống")
    private Boolean isActive;

    public UpdateSurveyStatusRequest() {
    }

    public UpdateSurveyStatusRequest(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
