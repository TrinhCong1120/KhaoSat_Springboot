package com.trinhcong1120.survey_service.dto.failed;

import java.time.LocalDateTime;

public class FailedSurveyResponse {

    private String fileName;
    private Long fileSize;
    private LocalDateTime lastModified;
    private String content;

    public FailedSurveyResponse() {
    }

    public FailedSurveyResponse(
            String fileName,
            Long fileSize,
            LocalDateTime lastModified,
            String content
    ) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(
            LocalDateTime lastModified
    ) {
        this.lastModified = lastModified;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}