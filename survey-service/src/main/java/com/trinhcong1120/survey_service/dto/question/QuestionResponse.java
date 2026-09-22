package com.trinhcong1120.survey_service.dto.question;

import java.util.List;

public class QuestionResponse {

    private Integer id;
    private Integer pageId;

    private String questionText;

    private Integer questionTypeId;
    private String questionTypeCode;

    private Boolean isRequired;
    private Integer orderIndex;

    private String description;

    private List<OptionResponse> options;

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
        this.id = id;
        this.pageId = pageId;
        this.questionText = questionText;
        this.questionTypeId = questionTypeId;
        this.questionTypeCode = questionTypeCode;
        this.isRequired = isRequired;
        this.orderIndex = orderIndex;
        this.description = description;
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

    public List<OptionResponse> getOptions() {
        return options;
    }

    public void setOptions(List<OptionResponse> options) {
        this.options = options;
    }
}