package com.trinhcong1120.survey_service.dto.response;

import java.util.List;

public class ResponsePageResponse {

    private Integer pageId;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String audioUrl;
    private Integer orderIndex;

    private List<ResponseQuestionResponse> questions;

    public ResponsePageResponse() {
    }

    public ResponsePageResponse(
            Integer pageId,
            String title,
            Integer orderIndex,
            List<ResponseQuestionResponse> questions
    ) {
        this(pageId, title, null, null, null, null, orderIndex, questions);
    }

    public ResponsePageResponse(
            Integer pageId,
            String title,
            String description,
            String imageUrl,
            String videoUrl,
            String audioUrl,
            Integer orderIndex,
            List<ResponseQuestionResponse> questions
    ) {
        this.pageId = pageId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.orderIndex = orderIndex;
        this.questions = questions;
    }

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
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
