package com.trinhcong1120.survey_service.dto.page;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.util.ArrayList;
import java.util.List;

public class PageResponse {

    private UUID id;
    private UUID surveyId;
    private String title;
    private String description;
    private Integer orderIndex;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public PageResponse() {
    }

    public PageResponse(
            UUID id,
            UUID surveyId,
            String title,
            Integer orderIndex
    ) {
        this(id, surveyId, title, null, orderIndex);
    }

    public PageResponse(
            UUID id,
            UUID surveyId,
            String title,
            String description,
            Integer orderIndex
    ) {
        this.id = id;
        this.surveyId = surveyId;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(UUID surveyId) {
        this.surveyId = surveyId;
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }
}
