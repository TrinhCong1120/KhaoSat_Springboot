package com.trinhcong1120.survey_service.dto.condition;

public class ConditionResponse {

    private Integer id;
    private Integer sourceQuestionId;
    private String sourceValue;
    private Integer targetQuestionId;
    private String action;

    public ConditionResponse() {
    }

    public ConditionResponse(
            Integer id,
            Integer sourceQuestionId,
            String sourceValue,
            Integer targetQuestionId,
            String action
    ) {
        this.id = id;
        this.sourceQuestionId = sourceQuestionId;
        this.sourceValue = sourceValue;
        this.targetQuestionId = targetQuestionId;
        this.action = action;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(Integer sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public Integer getTargetQuestionId() {
        return targetQuestionId;
    }

    public void setTargetQuestionId(Integer targetQuestionId) {
        this.targetQuestionId = targetQuestionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}