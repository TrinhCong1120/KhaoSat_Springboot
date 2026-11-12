package com.trinhcong1120.survey_service.dto.question;

import java.util.UUID;
import java.math.BigDecimal;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.validation.QuestionValidationRuleResponse;
import java.util.ArrayList;
import java.util.List;

public class QuestionResponse {

    private UUID id;
    private UUID pageId;

    private String questionText;

    private UUID questionTypeId;
    private String questionTypeCode;
    private QuestionTypeResponse questionType;

    private Boolean isRequired;
    private Integer orderIndex;

    private String description;

    private UUID parentId;
    private Integer level = 1;
    private String path = "";
    private Boolean isLeaf = true;
    private String aggregationType = "NONE";
    private String aggregationFormula;
    private BigDecimal weight = BigDecimal.ONE;
    private String matrixConfig;
    private List<QuestionResponse> children = new ArrayList<>();

    private List<OptionResponse> options;
    private List<QuestionValidationRuleResponse> validationRules = new ArrayList<>();
    private List<MediaUploadResponse> mediaFiles = new ArrayList<>();

    public QuestionResponse() {
    }

    public QuestionResponse(
            UUID id,
            UUID pageId,
            String questionText,
            UUID questionTypeId,
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
                null,
                isRequired,
                orderIndex,
                description,
                options
        );
    }

    public QuestionResponse(
            UUID id,
            UUID pageId,
            String questionText,
            UUID questionTypeId,
            String questionTypeCode,
            String questionTypeName,
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
        this.questionType = new QuestionTypeResponse(
                questionTypeId,
                questionTypeCode,
                questionTypeName
        );
        this.isRequired = isRequired;
        this.orderIndex = orderIndex;
        this.description = description;
        this.options = options;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPageId() {
        return pageId;
    }

    public void setPageId(UUID pageId) {
        this.pageId = pageId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
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

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level != null ? level : 1;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getPath() {
        return path != null ? path : "";
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getIsLeaf() {
        return isLeaf != null ? isLeaf : true;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public String getAggregationType() {
        return aggregationType != null ? aggregationType : "NONE";
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getAggregationFormula() {
        return aggregationFormula;
    }

    public void setAggregationFormula(String aggregationFormula) {
        this.aggregationFormula = aggregationFormula;
    }

    public BigDecimal getWeight() {
        return weight != null ? weight : BigDecimal.ONE;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getMatrixConfig() {
        return matrixConfig;
    }

    public void setMatrixConfig(String matrixConfig) {
        this.matrixConfig = matrixConfig;
    }

    public List<QuestionResponse> getChildren() {
        return children;
    }

    public void setChildren(List<QuestionResponse> children) {
        this.children = children == null ? new ArrayList<>() : children;
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
