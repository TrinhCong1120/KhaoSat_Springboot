package com.trinhcong1120.survey_service.dto.question;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = OptionRequestDeserializer.class)
public class OptionRequest {

    private String optionText;

    public OptionRequest() {
    }

    public OptionRequest(String optionText) {
        this.optionText = optionText;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }







}
