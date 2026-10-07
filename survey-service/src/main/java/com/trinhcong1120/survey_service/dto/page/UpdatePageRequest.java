package com.trinhcong1120.survey_service.dto.page;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdatePageRequest {

    @NotBlank(message = "Tiêu đề trang không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Thứ tự trang không được để trống")
    private Integer orderIndex;

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
}
