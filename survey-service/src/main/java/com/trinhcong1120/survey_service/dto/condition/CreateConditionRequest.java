package com.trinhcong1120.survey_service.dto.condition;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateConditionRequest {

    @NotNull(message = "ID câu hỏi nguồn không được để trống")
    private UUID sourceQuestionId;

    @NotBlank(message = "Giá trị điều kiện không được để trống")
    private String sourceValue;

    private String targetType = "QUESTION";

    private UUID targetQuestionId;

    private UUID targetPageId;

    @NotBlank(message = "Hành động điều kiện không được để trống")
    private String action;

    public CreateConditionRequest() {
    }

    public UUID getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(UUID sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getTargetType() {
        return targetType != null ? targetType : "QUESTION";
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public UUID getTargetQuestionId() {
        return targetQuestionId;
    }

    public void setTargetQuestionId(UUID targetQuestionId) {
        this.targetQuestionId = targetQuestionId;
    }

    public UUID getTargetPageId() {
        return targetPageId;
    }

    public void setTargetPageId(UUID targetPageId) {
        this.targetPageId = targetPageId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
