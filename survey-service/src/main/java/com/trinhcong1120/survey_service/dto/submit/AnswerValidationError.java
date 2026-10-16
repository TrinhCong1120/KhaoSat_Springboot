package com.trinhcong1120.survey_service.dto.submit;

import java.util.UUID;

public class AnswerValidationError {

    private UUID questionId;
    private String ruleCode;
    private String field;
    private String message;

    public AnswerValidationError() {
    }

    public AnswerValidationError(UUID questionId, String ruleCode, String message) {
        this(questionId, ruleCode, null, message);
    }

    public AnswerValidationError(
            UUID questionId,
            String ruleCode,
            String field,
            String message
    ) {
        this.questionId = questionId;
        this.ruleCode = ruleCode;
        this.field = field;
        this.message = message;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
