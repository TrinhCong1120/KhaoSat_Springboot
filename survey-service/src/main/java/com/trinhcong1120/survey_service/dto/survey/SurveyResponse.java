package com.trinhcong1120.survey_service.dto.survey;

import java.time.LocalDateTime;

public class SurveyResponse {

    private Integer id;
    private String title;
    private String description;
    private String creatorUser;
    private LocalDateTime createdAt;
    private Boolean isActive;

    public SurveyResponse() {
    }

    public SurveyResponse(
            Integer id,
            String title,
            String description,
            String creatorUser,
            LocalDateTime createdAt,
            Boolean isActive
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorUser = creatorUser;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorUser() {
        return creatorUser;
    }

    public void setCreatorUser(String creatorUser) {
        this.creatorUser = creatorUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}