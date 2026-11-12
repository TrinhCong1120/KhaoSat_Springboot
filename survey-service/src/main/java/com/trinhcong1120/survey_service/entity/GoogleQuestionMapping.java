package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_question_mappings")
public class GoogleQuestionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "survey_id", nullable = false)
    private UUID surveyId;

    @Column(name = "internal_question_id", nullable = false)
    private UUID internalQuestionId;

    @Column(name = "google_form_id", nullable = false, length = 255)
    private String googleFormId;

    @Column(name = "google_item_id", nullable = false, length = 255)
    private String googleItemId;

    @Column(name = "google_question_id", nullable = false, length = 255)
    private String googleQuestionId;

    @Column(name = "question_type", nullable = false, length = 64)
    private String questionType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public GoogleQuestionMapping() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSurveyId() { return surveyId; }
    public void setSurveyId(UUID surveyId) { this.surveyId = surveyId; }

    public UUID getInternalQuestionId() { return internalQuestionId; }
    public void setInternalQuestionId(UUID internalQuestionId) { this.internalQuestionId = internalQuestionId; }

    public String getGoogleFormId() { return googleFormId; }
    public void setGoogleFormId(String googleFormId) { this.googleFormId = googleFormId; }

    public String getGoogleItemId() { return googleItemId; }
    public void setGoogleItemId(String googleItemId) { this.googleItemId = googleItemId; }

    public String getGoogleQuestionId() { return googleQuestionId; }
    public void setGoogleQuestionId(String googleQuestionId) { this.googleQuestionId = googleQuestionId; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
