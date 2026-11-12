package com.trinhcong1120.survey_service.dto.question;

import java.util.UUID;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UpdateQuestionRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private UUID questionTypeId;

    private Boolean isRequired;

    @NotNull(message = "Thứ tự câu hỏi không được để trống")
    private Integer orderIndex;

    private String description;

    private UUID parentId;
    private String aggregationType = "NONE";
    private String aggregationFormula;
    private BigDecimal weight = BigDecimal.ONE;
    private String matrixConfig;

    private List<OptionRequest> options;

    public UpdateQuestionRequest() {
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

    public List<OptionRequest> getOptions() {
        return options;
    }

    public void setOptions(List<OptionRequest> options) {
        this.options = options;
    }
}
