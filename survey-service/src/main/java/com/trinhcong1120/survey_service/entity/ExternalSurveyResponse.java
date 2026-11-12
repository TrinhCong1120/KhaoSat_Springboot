package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_survey_responses")
public class ExternalSurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "survey_id", nullable = false)
    private UUID surveyId;

    @Column(name = "google_form_id", nullable = false, length = 255)
    private String googleFormId;

    @Column(name = "google_response_id", nullable = false, length = 255)
    private String googleResponseId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson = "{}";

    @Column(name = "internal_response_id")
    private UUID internalResponseId;

    @Column(name = "sync_status", nullable = false, length = 32)
    private String syncStatus = "SYNCED";

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    public ExternalSurveyResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSurveyId() { return surveyId; }
    public void setSurveyId(UUID surveyId) { this.surveyId = surveyId; }

    public String getGoogleFormId() { return googleFormId; }
    public void setGoogleFormId(String googleFormId) { this.googleFormId = googleFormId; }

    public String getGoogleResponseId() { return googleResponseId; }
    public void setGoogleResponseId(String googleResponseId) { this.googleResponseId = googleResponseId; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }

    public UUID getInternalResponseId() { return internalResponseId; }
    public void setInternalResponseId(UUID internalResponseId) { this.internalResponseId = internalResponseId; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
}
