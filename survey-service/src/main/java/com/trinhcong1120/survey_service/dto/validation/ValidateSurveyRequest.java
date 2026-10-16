package com.trinhcong1120.survey_service.dto.validation;

import com.trinhcong1120.survey_service.dto.submit.SubmitAnswerRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

public class ValidateSurveyRequest {

  private Long surveyRevision;

  @Valid
  private List<SubmitAnswerRequest> answers = new ArrayList<>();

  public Long getSurveyRevision() { return surveyRevision; }
  public void setSurveyRevision(Long surveyRevision) { this.surveyRevision = surveyRevision; }

  public List<SubmitAnswerRequest> getAnswers() { return answers; }
  public void setAnswers(List<SubmitAnswerRequest> answers) { this.answers = answers; }
}
