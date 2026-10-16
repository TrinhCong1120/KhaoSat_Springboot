package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.service.PublicSurveyService;
import com.trinhcong1120.survey_service.service.SurveyTrackingService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/surveys/{surveyRef}")
public class SurveyTrackingController {
  private final PublicSurveyService publicSurveys;
  private final SurveyTrackingService tracking;

  public SurveyTrackingController(PublicSurveyService publicSurveys, SurveyTrackingService tracking) {
    this.publicSurveys = publicSurveys;
    this.tracking = tracking;
  }

  @PostMapping("/sessions")
  public SurveyTrackingService.SessionResult createSession(@PathVariable String surveyRef,
      @RequestBody(required = false) SurveyTrackingService.CreateSession input) {
    return tracking.createSession(publicSurveys.resolveSurveyId(surveyRef), input);
  }

  @PostMapping("/events")
  public SurveyTrackingService.EventResult event(@PathVariable String surveyRef,
      @RequestBody SurveyTrackingService.ClientEvent input) {
    return tracking.record(publicSurveys.resolveSurveyId(surveyRef), input);
  }
}
