package com.trinhcong1120.survey_service.dto.question;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.util.ArrayList;
import java.util.List;

public class OptionResponse {

    private UUID id;
    private String optionText;
    private Integer orderIndex;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public OptionResponse() {
    }

    public OptionResponse(
            UUID id,
            String optionText,
            Integer orderIndex
    ) {
        this.id = id;
        this.optionText = optionText;
        this.orderIndex = orderIndex;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
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
