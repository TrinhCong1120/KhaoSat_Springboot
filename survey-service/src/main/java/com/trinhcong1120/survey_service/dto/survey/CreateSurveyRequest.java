package com.trinhcong1120.survey_service.dto.survey;

import jakarta.validation.constraints.NotBlank;

public class CreateSurveyRequest {

    @NotBlank(message = "Tiêu đề khảo sát không được để trống")
    private String title;

    private String description;
    private String imageUrl;
    private String videoUrl;
    private String audioUrl;

    public CreateSurveyRequest() {
    }

    public CreateSurveyRequest(String title, String description) {
        this(title, description, null, null, null);
    }

    public CreateSurveyRequest(
            String title,
            String description,
            String imageUrl,
            String videoUrl,
            String audioUrl
    ) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
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
}
