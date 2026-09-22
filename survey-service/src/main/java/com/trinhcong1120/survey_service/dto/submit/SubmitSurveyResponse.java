package com.trinhcong1120.survey_service.dto.submit;

import java.util.UUID;

public class SubmitSurveyResponse {

    private String message;
    private Integer responseId;
    private UUID requestId;

    public SubmitSurveyResponse() {
    }

    public SubmitSurveyResponse(
            String message,
            Integer responseId,
            UUID requestId
    ) {
        this.message = message;
        this.responseId = responseId;
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getResponseId() {
        return responseId;
    }

    public void setResponseId(Integer responseId) {
        this.responseId = responseId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
}