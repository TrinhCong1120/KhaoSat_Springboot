package com.trinhcong1120.survey_service.dto.failed;

public class ImportFailedSurveyResponse {

    private Integer total;
    private Integer imported;
    private Integer skippedDuplicate;
    private Integer failedRemain;

    public ImportFailedSurveyResponse() {
    }

    public ImportFailedSurveyResponse(
            Integer total,
            Integer imported,
            Integer skippedDuplicate,
            Integer failedRemain
    ) {
        this.total = total;
        this.imported = imported;
        this.skippedDuplicate = skippedDuplicate;
        this.failedRemain = failedRemain;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getImported() {
        return imported;
    }

    public void setImported(Integer imported) {
        this.imported = imported;
    }

    public Integer getSkippedDuplicate() {
        return skippedDuplicate;
    }

    public void setSkippedDuplicate(
            Integer skippedDuplicate
    ) {
        this.skippedDuplicate = skippedDuplicate;
    }

    public Integer getFailedRemain() {
        return failedRemain;
    }

    public void setFailedRemain(
            Integer failedRemain
    ) {
        this.failedRemain = failedRemain;
    }
}