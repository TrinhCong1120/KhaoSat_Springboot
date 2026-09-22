package com.trinhcong1120.survey_service.dto.statistics;

import java.time.LocalDateTime;

public class DateStatisticsResponse {

    private Long count;
    private LocalDateTime min;
    private LocalDateTime max;

    public DateStatisticsResponse() {
    }

    public DateStatisticsResponse(
            Long count,
            LocalDateTime min,
            LocalDateTime max
    ) {
        this.count = count;
        this.min = min;
        this.max = max;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public LocalDateTime getMin() {
        return min;
    }

    public void setMin(LocalDateTime min) {
        this.min = min;
    }

    public LocalDateTime getMax() {
        return max;
    }

    public void setMax(LocalDateTime max) {
        this.max = max;
    }
}