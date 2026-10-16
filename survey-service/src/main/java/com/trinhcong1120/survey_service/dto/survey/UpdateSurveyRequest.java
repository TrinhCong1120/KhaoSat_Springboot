package com.trinhcong1120.survey_service.dto.survey;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class UpdateSurveyRequest {

    @NotBlank(message = "Tiêu đề khảo sát không được để trống")
    private String title;

    private String description;
    private String shareCode;
    private boolean shareCodePresent;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private boolean openAtPresent;
    private boolean closeAtPresent;

    public UpdateSurveyRequest() {
    }

    public UpdateSurveyRequest(String title, String description) {
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

    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; this.shareCodePresent = true; }
    @com.fasterxml.jackson.annotation.JsonIgnore public boolean isShareCodePresent() { return shareCodePresent; }

    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; this.openAtPresent = true; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; this.closeAtPresent = true; }
    @com.fasterxml.jackson.annotation.JsonIgnore public boolean isOpenAtPresent() { return openAtPresent; }
    @com.fasterxml.jackson.annotation.JsonIgnore public boolean isCloseAtPresent() { return closeAtPresent; }

}
