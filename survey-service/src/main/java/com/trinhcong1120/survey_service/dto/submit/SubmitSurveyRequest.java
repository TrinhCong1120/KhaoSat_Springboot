package com.trinhcong1120.survey_service.dto.submit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class SubmitSurveyRequest {

    @Valid
    @NotEmpty(message = "Danh sách câu trả lời không được để trống")
    private List<SubmitAnswerRequest> answers = new ArrayList<>();

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
}
