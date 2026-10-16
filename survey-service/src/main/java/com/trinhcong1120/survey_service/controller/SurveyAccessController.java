package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.service.SurveyAccessService;
import com.trinhcong1120.survey_service.service.SurveyAccessService.AccessRequest;
import com.trinhcong1120.survey_service.service.SurveyAccessService.AccessResponse;
import com.trinhcong1120.survey_service.service.SurveyAccessService.OwnerRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Surveys/{surveyId}")
public class SurveyAccessController {
  private final SurveyAccessService service;

  public SurveyAccessController(SurveyAccessService service) { this.service = service; }

  @GetMapping("/access")
  @PreAuthorize("hasAuthority('survey_update_all') or @surveyAccessGuard.owns(#surveyId)")
  public ResponseEntity<List<AccessResponse>> list(@PathVariable UUID surveyId) {
    return ResponseEntity.ok(service.list(surveyId));
  }

  @PutMapping("/access/{userId}")
  @PreAuthorize("hasAuthority('survey_update_all') or @surveyAccessGuard.owns(#surveyId)")
  public ResponseEntity<AccessResponse> put(@PathVariable UUID surveyId, @PathVariable UUID userId,
      @RequestBody AccessRequest request) {
    return ResponseEntity.ok(service.put(surveyId, userId, request));
  }

  @DeleteMapping("/access/{userId}")
  @PreAuthorize("hasAuthority('survey_update_all') or @surveyAccessGuard.owns(#surveyId)")
  public ResponseEntity<Void> remove(@PathVariable UUID surveyId, @PathVariable UUID userId) {
    service.remove(surveyId, userId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/owner")
  @PreAuthorize("hasAuthority('survey_update_all') or @surveyAccessGuard.owns(#surveyId)")
  public ResponseEntity<Void> changeOwner(@PathVariable UUID surveyId, @RequestBody OwnerRequest request) {
    service.changeOwner(surveyId, request);
    return ResponseEntity.noContent().build();
  }
}
