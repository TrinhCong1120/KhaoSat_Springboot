package com.trinhcong1120.survey_service.dto.question;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateQuestionRequest {

    @NotNull(message = "ID trang không được để trống")
    private UUID pageId;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private UUID questionTypeId;

    private Boolean isRequired;

    @NotNull(message = "Thứ tự câu hỏi không được để trống")
    private Integer orderIndex;

    private String description;
    private List<OptionRequest> options;

    public CreateQuestionRequest() {
    }

    public UUID getPageId() {
        return pageId;
    }

    public void setPageId(UUID pageId) {
        this.pageId = pageId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public UUID getQuestionTypeId() {
        return questionTypeId;
    }

    public void setQuestionTypeId(UUID questionTypeId) {
        this.questionTypeId = questionTypeId;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean required) {
        isRequired = required;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<OptionRequest> getOptions() {
        return options;
    }

    public void setOptions(List<OptionRequest> options) {
        this.options = options;
    }

}
