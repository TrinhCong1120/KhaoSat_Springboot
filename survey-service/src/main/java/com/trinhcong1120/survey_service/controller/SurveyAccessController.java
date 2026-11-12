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
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;

  public SurveyAccessController(SurveyAccessService service,
      com.trinhcong1120.survey_service.security.SurveyAccessGuard guard) {
    this.service = service; this.guard = guard;
  }

  @GetMapping("/access")
  @PreAuthorize("@surveyAccessGuard.canManageAccess(#surveyId)")
  public ResponseEntity<List<AccessResponse>> list(@PathVariable UUID surveyId) {
    return ResponseEntity.ok(service.list(surveyId));
  }

  @GetMapping("/access/effective")
  @PreAuthorize("@surveyAccessGuard.canManageAccess(#surveyId)")
  public java.util.Set<String> effective(@PathVariable UUID surveyId) {
    return guard.effectiveSurveyPermissions(surveyId);
  }

  @GetMapping("/access/users/search")
  @PreAuthorize("@surveyAccessGuard.canManageAccess(#surveyId)")
  public ResponseEntity<List<SurveyAccessService.UserRow>> search(@PathVariable UUID surveyId,
      @RequestParam String q) {
    return ResponseEntity.ok(service.searchUsers(surveyId, q));
  }

  @PutMapping("/access/{userId}")
  @PreAuthorize("@surveyAccessGuard.canManageAccess(#surveyId)")
  public ResponseEntity<AccessResponse> put(@PathVariable UUID surveyId, @PathVariable UUID userId,
      @RequestBody AccessRequest request) {
    return ResponseEntity.ok(service.put(surveyId, userId, request));
  }

  @DeleteMapping("/access/{userId}")
  @PreAuthorize("@surveyAccessGuard.canManageAccess(#surveyId)")
  public ResponseEntity<Void> remove(@PathVariable UUID surveyId, @PathVariable UUID userId) {
    service.remove(surveyId, userId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/owner")
  @PreAuthorize("@surveyAccessGuard.has('survey_update_all')")
  public ResponseEntity<Void> changeOwner(@PathVariable UUID surveyId, @RequestBody OwnerRequest request) {
    service.changeOwner(surveyId, request);
    return ResponseEntity.noContent().build();
  }
}
