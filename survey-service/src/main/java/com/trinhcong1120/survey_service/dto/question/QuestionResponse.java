package com.trinhcong1120.survey_service.dto.question;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.validation.QuestionValidationRuleResponse;
import java.util.ArrayList;
import java.util.List;

public class QuestionResponse {

    private Integer id;
    private Integer pageId;

    private String questionText;

    private Integer questionTypeId;
    private String questionTypeCode;
    private QuestionTypeResponse questionType;

    private Boolean isRequired;
    private Integer orderIndex;

    private String description;
    private String imageUrl;
    private String videoUrl;
    private String audioUrl;

    private List<OptionResponse> options;
    private List<QuestionValidationRuleResponse> validationRules = new ArrayList<>();
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public QuestionResponse() {
    }

    public QuestionResponse(
            Integer id,
            Integer pageId,
            String questionText,
            Integer questionTypeId,
            String questionTypeCode,
            Boolean isRequired,
            Integer orderIndex,
            String description,
            List<OptionResponse> options
    ) {
        this(
                id,
                pageId,
                questionText,
                questionTypeId,
                questionTypeCode,
                isRequired,
                orderIndex,
                description,
                null,
                null,
                null,
                options
        );
    }

    public QuestionResponse(
            Integer id,
            Integer pageId,
            String questionText,
            Integer questionTypeId,
            String questionTypeCode,
            Boolean isRequired,
            Integer orderIndex,
            String description,
            String imageUrl,
            String videoUrl,
            String audioUrl,
            List<OptionResponse> options
    ) {
        this(
                id,
                pageId,
                questionText,
                questionTypeId,
                questionTypeCode,
                null,
                isRequired,
                orderIndex,
                description,
                imageUrl,
                videoUrl,
                audioUrl,
                options
        );
    }

    public QuestionResponse(
            Integer id,
            Integer pageId,
            String questionText,
            Integer questionTypeId,
            String questionTypeCode,
            String questionTypeName,
            Boolean isRequired,
            Integer orderIndex,
            String description,
            List<OptionResponse> options
    ) {
        this(
                id,
                pageId,
                questionText,
                questionTypeId,
                questionTypeCode,
                questionTypeName,
                isRequired,
                orderIndex,
                description,
                null,
                null,
                null,
                options
        );
    }

    public QuestionResponse(
            Integer id,
            Integer pageId,
            String questionText,
            Integer questionTypeId,
            String questionTypeCode,
            String questionTypeName,
            Boolean isRequired,
            Integer orderIndex,
            String description,
            String imageUrl,
            String videoUrl,
            String audioUrl,
            List<OptionResponse> options
    ) {
        this.id = id;
        this.pageId = pageId;
        this.questionText = questionText;
        this.questionTypeId = questionTypeId;
        this.questionTypeCode = questionTypeCode;
        this.questionType = new QuestionTypeResponse(
                questionTypeId,
                questionTypeCode,
                questionTypeName
        );
        this.isRequired = isRequired;
        this.orderIndex = orderIndex;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.options = options;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getQuestionTypeId() {
        return questionTypeId;
    }

    public void setQuestionTypeId(Integer questionTypeId) {
        this.questionTypeId = questionTypeId;
    }

    public String getQuestionTypeCode() {
        return questionTypeCode;
    }

    public void setQuestionTypeCode(String questionTypeCode) {
        this.questionTypeCode = questionTypeCode;
    }

    public QuestionTypeResponse getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionTypeResponse questionType) {
        this.questionType = questionType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean required) {
        isRequired = required;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
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

    public List<OptionResponse> getOptions() {
        return options;
    }

    public void setOptions(List<OptionResponse> options) {
        this.options = options;
    }

    public List<QuestionValidationRuleResponse> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(List<QuestionValidationRuleResponse> validationRules) {
        this.validationRules = validationRules == null ? new ArrayList<>() : validationRules;
    }

    public List<MediaUploadResponse> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaUploadResponse> mediaFiles) {
        this.mediaFiles = mediaFiles == null ? new ArrayList<>() : mediaFiles;
    }
}
