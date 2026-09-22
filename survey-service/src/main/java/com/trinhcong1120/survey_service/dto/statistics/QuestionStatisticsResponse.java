package com.trinhcong1120.survey_service.dto.statistics;

public class QuestionStatisticsResponse {

    private Integer questionId;
    private String questionText;
    private String questionTypeCode;

    private ChoiceStatisticsResponse choiceStatistics;
    private NumberStatisticsResponse numberStatistics;
    private DateStatisticsResponse dateStatistics;
    private AddressStatisticsResponse addressStatistics;
    private TextStatisticsResponse textStatistics;

    public QuestionStatisticsResponse() {
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

    public String getQuestionTypeCode() {
        return questionTypeCode;
    }

    public void setQuestionTypeCode(String questionTypeCode) {
        this.questionTypeCode = questionTypeCode;
    }

    public ChoiceStatisticsResponse getChoiceStatistics() {
        return choiceStatistics;
    }

    public void setChoiceStatistics(ChoiceStatisticsResponse choiceStatistics) {
        this.choiceStatistics = choiceStatistics;
    }

    public NumberStatisticsResponse getNumberStatistics() {
        return numberStatistics;
    }

    public void setNumberStatistics(NumberStatisticsResponse numberStatistics) {
        this.numberStatistics = numberStatistics;
    }

    public DateStatisticsResponse getDateStatistics() {
        return dateStatistics;
    }

    public void setDateStatistics(DateStatisticsResponse dateStatistics) {
        this.dateStatistics = dateStatistics;
    }

    public AddressStatisticsResponse getAddressStatistics() {
        return addressStatistics;
    }

    public void setAddressStatistics(AddressStatisticsResponse addressStatistics) {
        this.addressStatistics = addressStatistics;
    }

    public TextStatisticsResponse getTextStatistics() {
        return textStatistics;
    }

    public void setTextStatistics(TextStatisticsResponse textStatistics) {
        this.textStatistics = textStatistics;
    }
}