package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.submit.SubmitSurveyRequest;
import com.trinhcong1120.survey_service.dto.submit.SubmitSurveyResponse;
import com.trinhcong1120.survey_service.dto.survey.SurveyDetailResponse;
import com.trinhcong1120.survey_service.dto.validation.ValidateSurveyRequest;
import com.trinhcong1120.survey_service.dto.validation.ValidateSurveyResponse;
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

  @GetMapping("/{surveyRef}")
  public ResponseEntity<SurveyDetailResponse> getSurvey(
          @PathVariable String surveyRef
  ) {
    return ResponseEntity.ok(
            publicSurveyService
                    .getActiveSurvey(publicSurveyService.resolveSurveyId(surveyRef))
    );
  }

  @PostMapping("/{surveyRef}/submit")
  public ResponseEntity<SubmitSurveyResponse> submit(
          @PathVariable String surveyRef,
          @Valid @RequestBody SubmitSurveyRequest request
  ) {
    return ResponseEntity.ok(
            publicSurveyService.submit(
                    publicSurveyService.resolveSurveyId(surveyRef),
                    request
            )
    );
  }

  @PostMapping("/{surveyRef}/validate")
  public ResponseEntity<ValidateSurveyResponse> validate(
          @PathVariable String surveyRef,
          @Valid @RequestBody ValidateSurveyRequest request
  ) {
    return ResponseEntity.ok(
            publicSurveyService.validate(
                    publicSurveyService.resolveSurveyId(surveyRef),
                    request
            )
    );
  }
}
