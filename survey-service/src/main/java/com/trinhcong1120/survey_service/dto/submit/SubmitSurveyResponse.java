package com.trinhcong1120.survey_service.dto.submit;

import java.util.UUID;

public class SubmitSurveyResponse {

    private String message;
    private UUID responseId;
    private UUID requestId;

    public SubmitSurveyResponse() {
    }

    public SubmitSurveyResponse(
            String message,
            UUID responseId,
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

    public UUID getResponseId() {
        return responseId;
    }

    public void setResponseId(UUID responseId) {
        this.responseId = responseId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
}