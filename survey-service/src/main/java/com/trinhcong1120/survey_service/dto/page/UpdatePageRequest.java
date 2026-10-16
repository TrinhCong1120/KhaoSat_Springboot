package com.trinhcong1120.survey_service.dto.page;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class UpdatePageRequest {

    @NotBlank(message = "Tiêu đề trang không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Thứ tự trang không được để trống")
    private Integer orderIndex;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private boolean openAtPresent;
    private boolean closeAtPresent;

    public UpdatePageRequest() {
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
    public LocalDateTime getOpenAt() { return openAt; }
    public void setOpenAt(LocalDateTime openAt) { this.openAt = openAt; this.openAtPresent = true; }
    public LocalDateTime getCloseAt() { return closeAt; }
    public void setCloseAt(LocalDateTime closeAt) { this.closeAt = closeAt; this.closeAtPresent = true; }
    @com.fasterxml.jackson.annotation.JsonIgnore public boolean isOpenAtPresent() { return openAtPresent; }
    @com.fasterxml.jackson.annotation.JsonIgnore public boolean isCloseAtPresent() { return closeAtPresent; }
}
