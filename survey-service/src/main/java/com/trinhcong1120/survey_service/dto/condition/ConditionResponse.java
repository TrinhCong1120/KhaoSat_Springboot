package com.trinhcong1120.survey_service.dto.condition;

import java.util.UUID;

public class ConditionResponse {

    private UUID id;
    private UUID sourceQuestionId;
    private String sourceValue;
    private String targetType = "QUESTION";
    private UUID targetQuestionId;
    private UUID targetPageId;
    private String action;

    public ConditionResponse() {
    }

    public ConditionResponse(
            UUID id,
            UUID sourceQuestionId,
            String sourceValue,
            UUID targetQuestionId,
            String action
    ) {
        this(id, sourceQuestionId, sourceValue, "QUESTION", targetQuestionId, null, action);
    }

    public ConditionResponse(
            UUID id,
            UUID sourceQuestionId,
            String sourceValue,
            String targetType,
            UUID targetQuestionId,
            UUID targetPageId,
            String action
    ) {
        this.id = id;
        this.sourceQuestionId = sourceQuestionId;
        this.sourceValue = sourceValue;
        this.targetType = targetType != null ? targetType : "QUESTION";
        this.targetQuestionId = targetQuestionId;
        this.targetPageId = targetPageId;
        this.action = action;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
        return targetType;
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
