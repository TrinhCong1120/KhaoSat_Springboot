package com.trinhcong1120.survey_service.dto.statistics;

import java.util.UUID;

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

        private UUID optionId;
        private String optionText;
        private Long count;
        private Double percentage;

        public OptionStatistic() {
        }

        public OptionStatistic(
                UUID optionId,
                String optionText,
                Long count,
                Double percentage
        ) {
            this.optionId = optionId;
            this.optionText = optionText;
            this.count = count;
            this.percentage = percentage;
        }

        public UUID getOptionId() {
            return optionId;
        }

        public void setOptionId(UUID optionId) {
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