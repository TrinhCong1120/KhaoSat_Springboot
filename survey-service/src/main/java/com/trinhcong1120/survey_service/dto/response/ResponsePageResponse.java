package com.trinhcong1120.survey_service.dto.response;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.util.ArrayList;
import java.util.UUID;

import java.util.List;

public class ResponsePageResponse {

    private UUID pageId;
    private String title;
    private String description;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();
    private Integer orderIndex;

    private List<ResponseQuestionResponse> questions;

    public ResponsePageResponse() {
    }

    public ResponsePageResponse(
            UUID pageId,
            String title,
            Integer orderIndex,
            List<ResponseQuestionResponse> questions
    ) {
        this(pageId, title, null, orderIndex, questions);
    }

    public ResponsePageResponse(
            UUID pageId,
            String title,
            String description,
            Integer orderIndex,
            List<ResponseQuestionResponse> questions
    ) {
        this.pageId = pageId;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.questions = questions;
    }

    public UUID getPageId() {
        return pageId;
    }

    public void setPageId(UUID pageId) {
        this.pageId = pageId;
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

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public List<ResponseQuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ResponseQuestionResponse> questions) {
        this.questions = questions;
    }
}
