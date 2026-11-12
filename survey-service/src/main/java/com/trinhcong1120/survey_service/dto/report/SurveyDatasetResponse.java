package com.trinhcong1120.survey_service.dto.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SurveyDatasetResponse {
    private UUID surveyId;
    private String surveyTitle;
    private long totalResponses;
    private boolean canExportData;
    private boolean piiIncluded;
    private List<UUID> piiQuestionIds = new ArrayList<>();
    private Map<UUID, String> piiTypes = new HashMap<>();
    private List<RecordItem> records = new ArrayList<>();

    public SurveyDatasetResponse() {}

    public SurveyDatasetResponse(UUID surveyId, String surveyTitle, long totalResponses, List<RecordItem> records) {
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
        this.totalResponses = totalResponses;
        this.records = records;
    }

    public static class AnswerValue {
        private String text;
        private Double number;
        private String date;
        private List<String> optionTexts;
        private List<String> optionIds;
        private String province;
        private String ward;
        private String addressDetail;
        private String formatted;
        private String matrixData;

        public AnswerValue() {}

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Double getNumber() { return number; }
        public void setNumber(Double number) { this.number = number; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public List<String> getOptionTexts() { return optionTexts; }
        public void setOptionTexts(List<String> optionTexts) { this.optionTexts = optionTexts; }

        public List<String> getOptionIds() { return optionIds; }
        public void setOptionIds(List<String> optionIds) { this.optionIds = optionIds; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }

        public String getAddressDetail() { return addressDetail; }
        public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }

        public String getFormatted() { return formatted; }
        public void setFormatted(String formatted) { this.formatted = formatted; }

        public String getMatrixData() { return matrixData; }
        public void setMatrixData(String matrixData) { this.matrixData = matrixData; }
    }

    public static class RecordItem {
        private UUID id;
        private UUID requestId;
        private LocalDateTime submittedAt;
        private Long durationMs;
        private Double totalScore;
        private Map<String, AnswerValue> answers = new HashMap<>();

        public RecordItem() {}

        public RecordItem(UUID id, UUID requestId, LocalDateTime submittedAt, Map<String, AnswerValue> answers) {
            this.id = id;
            this.requestId = requestId;
            this.submittedAt = submittedAt;
            this.answers = answers;
        }

        public RecordItem(
                UUID id,
                UUID requestId,
                LocalDateTime submittedAt,
                Long durationMs,
                Double totalScore,
                Map<String, AnswerValue> answers) {
            this.id = id;
            this.requestId = requestId;
            this.submittedAt = submittedAt;
            this.durationMs = durationMs;
            this.totalScore = totalScore;
            this.answers = answers;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getRequestId() { return requestId; }
        public void setRequestId(UUID requestId) { this.requestId = requestId; }

        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

        public Long getDurationMs() { return durationMs; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

        public Double getTotalScore() { return totalScore; }
        public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

        public Map<String, AnswerValue> getAnswers() { return answers; }
        public void setAnswers(Map<String, AnswerValue> answers) { this.answers = answers; }
    }

    public UUID getSurveyId() { return surveyId; }
    public void setSurveyId(UUID surveyId) { this.surveyId = surveyId; }

    public String getSurveyTitle() { return surveyTitle; }
    public void setSurveyTitle(String surveyTitle) { this.surveyTitle = surveyTitle; }

    public long getTotalResponses() { return totalResponses; }
    public void setTotalResponses(long totalResponses) { this.totalResponses = totalResponses; }

    public boolean isCanExportData() { return canExportData; }
    public void setCanExportData(boolean canExportData) { this.canExportData = canExportData; }

    public boolean isPiiIncluded() { return piiIncluded; }
    public void setPiiIncluded(boolean piiIncluded) { this.piiIncluded = piiIncluded; }

    public List<UUID> getPiiQuestionIds() { return piiQuestionIds; }
    public void setPiiQuestionIds(List<UUID> piiQuestionIds) { this.piiQuestionIds = piiQuestionIds; }

    public Map<UUID, String> getPiiTypes() { return piiTypes; }
    public void setPiiTypes(Map<UUID, String> piiTypes) { this.piiTypes = piiTypes; }

    public List<RecordItem> getRecords() { return records; }
    public void setRecords(List<RecordItem> records) { this.records = records; }
}

