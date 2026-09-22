package com.trinhcong1120.survey_service.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResponseQuestionResponse {

    private Integer questionId;
    private String questionText;

    private Integer questionTypeId;
    private String questionTypeCode;

    private Boolean isRequired;
    private Boolean isApplicable;

    private String answerText;
    private Double answerNumber;
    private LocalDateTime answerDate;

    private Integer provinceCode;
    private Integer wardCode;

    private String province;
    private String ward;

    private List<Integer> optionIds = new ArrayList<>();
    private List<String> optionTexts = new ArrayList<>();

    private String formattedAnswer;

    public ResponseQuestionResponse() {
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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

    public Double getAnswerNumber() {
        return answerNumber;
    }

    public void setAnswerNumber(Double answerNumber) {
        this.answerNumber = answerNumber;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public Integer getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(Integer provinceCode) {
        this.provinceCode = provinceCode;
    }

    public Integer getWardCode() {
        return wardCode;
    }

    public void setWardCode(Integer wardCode) {
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

    public List<Integer> getOptionIds() {
        return optionIds;
    }

    public void setOptionIds(List<Integer> optionIds) {
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