package com.trinhcong1120.survey_service.dto.submit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SubmitSurveyRequest {

    @Valid
    @NotEmpty(message = "Danh sach cau tra loi khong duoc de trong")
    private List<SubmitAnswerRequest> answers = new ArrayList<>();

    private String token;
    private UUID requestId;

    public SubmitSurveyRequest() {
    }

    public SubmitSurveyRequest(List<SubmitAnswerRequest> answers) {
        this.answers = answers;
    }

    public List<SubmitAnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SubmitAnswerRequest> answers) {
        this.answers = answers;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
}
