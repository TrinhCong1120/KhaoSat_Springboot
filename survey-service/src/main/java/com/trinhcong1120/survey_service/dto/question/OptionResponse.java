package com.trinhcong1120.survey_service.dto.question;

public class OptionResponse {

    private Integer id;
    private String optionText;
    private Integer orderIndex;

    public OptionResponse() {
    }

    public OptionResponse(
            Integer id,
            String optionText,
            Integer orderIndex
    ) {
        this.id = id;
        this.optionText = optionText;
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}