package com.trinhcong1120.survey_service.dto.question;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.util.ArrayList;
import java.util.List;

public class OptionResponse {

    private Integer id;
    private String optionText;
    private String imageUrl;
    private String videoUrl;
    private String audioUrl;
    private Integer orderIndex;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public OptionResponse() {
    }

    public OptionResponse(
            Integer id,
            String optionText,
            Integer orderIndex
    ) {
        this(id, optionText, null, null, null, orderIndex);
    }

    public OptionResponse(
            Integer id,
            String optionText,
            String imageUrl,
            String videoUrl,
            String audioUrl,
            Integer orderIndex
    ) {
        this.id = id;
        this.optionText = optionText;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.orderIndex = orderIndex;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
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

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }
}
