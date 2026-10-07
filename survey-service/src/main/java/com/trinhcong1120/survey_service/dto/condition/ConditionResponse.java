package com.trinhcong1120.survey_service.dto.condition;

import java.util.UUID;

public class ConditionResponse {

    private UUID id;
    private UUID sourceQuestionId;
    private String sourceValue;
    private UUID targetQuestionId;
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
        this.id = id;
        this.sourceQuestionId = sourceQuestionId;
        this.sourceValue = sourceValue;
        this.targetQuestionId = targetQuestionId;
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