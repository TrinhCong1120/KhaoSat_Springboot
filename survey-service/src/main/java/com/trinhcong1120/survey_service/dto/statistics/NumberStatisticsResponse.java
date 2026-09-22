package com.trinhcong1120.survey_service.dto.statistics;

public class NumberStatisticsResponse {

    private Long count;
    private Double average;
    private Double min;
    private Double max;
    private Double median;

    public NumberStatisticsResponse() {
    }

    public NumberStatisticsResponse(
            Long count,
            Double average,
            Double min,
            Double max,
            Double median
    ) {
        this.count = count;
        this.average = average;
        this.min = min;
        this.max = max;
        this.median = median;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getAverage() {
        return average;
    }

    public void setAverage(Double average) {
        this.average = average;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }
}