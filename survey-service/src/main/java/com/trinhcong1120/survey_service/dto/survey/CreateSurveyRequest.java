package com.trinhcong1120.survey_service.dto.survey;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class CreateSurveyRequest {

    @NotBlank(message = "Tiêu đề khảo sát không được để trống")
    private String title;

    private String description;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;

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

    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; }

}
