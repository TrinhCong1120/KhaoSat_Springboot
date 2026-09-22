package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.submit.SubmitSurveyRequest;
import com.trinhcong1120.survey_service.dto.submit.SubmitSurveyResponse;
import com.trinhcong1120.survey_service.dto.survey.SurveyDetailResponse;
import com.trinhcong1120.survey_service.service.PublicSurveyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/surveys")
public class PublicSurveyController {

  private final PublicSurveyService publicSurveyService;

  public PublicSurveyController(
          PublicSurveyService publicSurveyService
  ) {
    this.publicSurveyService = publicSurveyService;
  }

  @GetMapping("/{surveyId}")
  public ResponseEntity<SurveyDetailResponse> getSurvey(
          @PathVariable Integer surveyId
  ) {
    return ResponseEntity.ok(
            publicSurveyService
                    .getActiveSurvey(surveyId)
    );
  }

  @PostMapping("/{surveyId}/submit")
  public ResponseEntity<SubmitSurveyResponse> submit(
          @PathVariable Integer surveyId,
          @Valid @RequestBody SubmitSurveyRequest request
  ) {
    return ResponseEntity.ok(
            publicSurveyService.submit(
                    surveyId,
                    request
            )
    );
  }
}
