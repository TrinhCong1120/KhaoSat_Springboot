package com.trinhcong1120.survey_service.dto.condition;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateConditionRequest {

    @NotNull(message = "ID câu hỏi nguồn không được để trống")
    private UUID sourceQuestionId;

    @NotBlank(message = "Giá trị điều kiện không được để trống")
    private String sourceValue;

    @NotNull(message = "ID câu hỏi đích không được để trống")
    private UUID targetQuestionId;

    @NotBlank(message = "Hành động điều kiện không được để trống")
    private String action;

    public UpdateConditionRequest() {
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

    public UUID getTargetQuestionId() {
        return targetQuestionId;
    }

    public void setTargetQuestionId(UUID targetQuestionId) {
        this.targetQuestionId = targetQuestionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
