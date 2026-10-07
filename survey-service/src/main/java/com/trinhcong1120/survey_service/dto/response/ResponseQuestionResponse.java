package com.trinhcong1120.survey_service.dto.response;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResponseQuestionResponse {

    private UUID questionId;
    private String questionText;
    private String description;
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    private UUID questionTypeId;
    private String questionTypeCode;

    private Boolean isRequired;
    private Boolean isApplicable;

    private String answerText;
    private BigDecimal answerNumber;
    private LocalDateTime answerDate;

    private String provinceCode;
    private String wardCode;

    private String province;
    private String ward;
    private String addressDetail;

    private List<UUID> optionIds = new ArrayList<>();
    private List<String> optionTexts = new ArrayList<>();

    private String formattedAnswer;

    public ResponseQuestionResponse() {
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
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

    public UUID getQuestionTypeId() {
        return questionTypeId;
    }

    public void setQuestionTypeId(UUID questionTypeId) {
        this.questionTypeId = questionTypeId;
    }

    public String getQuestionTypeCode() {
        return questionTypeCode;
    }

    public void setQuestionTypeCode(String questionTypeCode) {
        this.questionTypeCode = questionTypeCode;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean required) {
        isRequired = required;
    }

    public Boolean getIsApplicable() {
        return isApplicable;
    }

    public void setIsApplicable(Boolean applicable) {
        isApplicable = applicable;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public BigDecimal getAnswerNumber() {
        return answerNumber;
    }

    public void setAnswerNumber(BigDecimal answerNumber) {
        this.answerNumber = answerNumber;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public List<UUID> getOptionIds() {
        return optionIds;
    }

    public void setOptionIds(List<UUID> optionIds) {
        this.optionIds = optionIds;
    }

    public List<String> getOptionTexts() {
        return optionTexts;
    }

    public void setOptionTexts(List<String> optionTexts) {
        this.optionTexts = optionTexts;
    }

    public String getFormattedAnswer() {
        return formattedAnswer;
    }

    public void setFormattedAnswer(String formattedAnswer) {
        this.formattedAnswer = formattedAnswer;
    }
}
