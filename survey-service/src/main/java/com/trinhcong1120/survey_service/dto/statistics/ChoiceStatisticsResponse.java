package com.trinhcong1120.survey_service.dto.statistics;

import java.util.List;

public class ChoiceStatisticsResponse {

    private List<OptionStatistic> options;

    public ChoiceStatisticsResponse() {
    }

    public ChoiceStatisticsResponse(List<OptionStatistic> options) {
        this.options = options;
    }

    public List<OptionStatistic> getOptions() {
        return options;
    }

    public void setOptions(List<OptionStatistic> options) {
        this.options = options;
    }

    public static class OptionStatistic {

        private Integer optionId;
        private String optionText;
        private Long count;
        private Double percentage;

        public OptionStatistic() {
        }

        public OptionStatistic(
                Integer optionId,
                String optionText,
                Long count,
                Double percentage
        ) {
            this.optionId = optionId;
            this.optionText = optionText;
            this.count = count;
            this.percentage = percentage;
        }

        public Integer getOptionId() {
            return optionId;
        }

        public void setOptionId(Integer optionId) {
            this.optionId = optionId;
        }

        public String getOptionText() {
            return optionText;
        }

        public void setOptionText(String optionText) {
            this.optionText = optionText;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
}