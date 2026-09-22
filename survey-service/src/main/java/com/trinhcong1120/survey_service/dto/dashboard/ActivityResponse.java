package com.trinhcong1120.survey_service.dto.dashboard;

import java.time.LocalDate;

public class ActivityResponse {

    private LocalDate date;
    private String label;
    private Long responses;
    private Long views;

    public ActivityResponse() {
    }

    public ActivityResponse(
            LocalDate date,
            String label,
            Long responses,
            Long views
    ) {
        this.date = date;
        this.label = label;
        this.responses = responses;
        this.views = views;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getResponses() {
        return responses;
    }

    public void setResponses(Long responses) {
        this.responses = responses;
    }

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = views;
    }
}