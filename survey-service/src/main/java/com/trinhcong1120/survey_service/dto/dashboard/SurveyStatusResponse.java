package com.trinhcong1120.survey_service.dto.dashboard;

public class SurveyStatusResponse {

    private String status;
    private Long count;

    public SurveyStatusResponse() {
    }

    public SurveyStatusResponse(
            String status,
            Long count
    ) {
        this.status = status;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}