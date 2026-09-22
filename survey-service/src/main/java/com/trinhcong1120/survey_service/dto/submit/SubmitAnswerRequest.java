package com.trinhcong1120.survey_service.dto.submit;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SubmitAnswerRequest {

    @NotNull(message = "ID câu hỏi không được để trống")
    private Integer questionId;

    private String answerText;
    private Double answerNumber;
    private LocalDateTime answerDate;

    private Integer provinceCode;
    private Integer wardCode;

    private String province;
    private String ward;

    private List<Integer> optionIds = new ArrayList<>();

    public SubmitAnswerRequest() {
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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
}
