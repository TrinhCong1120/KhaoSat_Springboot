package com.trinhcong1120.survey_service.dto.validation;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.submit.AnswerValidationError;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ValidateSurveyResponse {

  private Boolean valid;
  private Long surveyRevision;
  private Instant evaluatedAt;
  private List<QuestionState> questionStates = new ArrayList<>();
  private List<AnswerValidationError> fieldErrors = new ArrayList<>();

  public Boolean getValid() { return valid; }
  public void setValid(Boolean valid) { this.valid = valid; }

  public Long getSurveyRevision() { return surveyRevision; }
  public void setSurveyRevision(Long surveyRevision) { this.surveyRevision = surveyRevision; }

  public Instant getEvaluatedAt() { return evaluatedAt; }
  public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }

  public List<QuestionState> getQuestionStates() { return questionStates; }
  public void setQuestionStates(List<QuestionState> questionStates) { this.questionStates = questionStates; }

  public List<AnswerValidationError> getFieldErrors() { return fieldErrors; }
  public void setFieldErrors(List<AnswerValidationError> fieldErrors) { this.fieldErrors = fieldErrors; }

  public static class QuestionState {
    private UUID questionId;
    private Boolean visible;
    private Boolean applicable;

    public QuestionState() {}

    public QuestionState(UUID questionId, Boolean visible, Boolean applicable) {
      this.questionId = questionId;
      this.visible = visible;
      this.applicable = applicable;
    }

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }

    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }

    public Boolean getApplicable() { return applicable; }
    public void setApplicable(Boolean applicable) { this.applicable = applicable; }
  }
}
