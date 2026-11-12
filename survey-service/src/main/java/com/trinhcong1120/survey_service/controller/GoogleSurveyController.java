package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.survey.GoogleDriveFormItem;
import com.trinhcong1120.survey_service.dto.survey.GoogleIntegrationDto;
import com.trinhcong1120.survey_service.dto.survey.SurveyResponse;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import com.trinhcong1120.survey_service.service.GoogleFormAccessService;
import com.trinhcong1120.survey_service.service.GoogleResponseSyncService;
import com.trinhcong1120.survey_service.service.GoogleSurveyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/Surveys")
public class GoogleSurveyController {

    private final GoogleSurveyService googleSurveyService;
    private final GoogleFormAccessService accessService;
    private final GoogleResponseSyncService syncService;
    private final SurveyAccessGuard accessGuard;

    public GoogleSurveyController(
            GoogleSurveyService googleSurveyService,
            GoogleFormAccessService accessService,
            GoogleResponseSyncService syncService,
            SurveyAccessGuard accessGuard
    ) {
        this.googleSurveyService = googleSurveyService;
        this.accessService = accessService;
        this.syncService = syncService;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/google/create")
    public ResponseEntity<SurveyResponse> createGoogleForm(
            @Valid @RequestBody GoogleSurveyService.CreateGoogleFormRequest request,
            HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        SurveyResponse response = googleSurveyService.createGoogleForm(request, authHeader);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google/link")
    public ResponseEntity<SurveyResponse> linkGoogleForm(
            @Valid @RequestBody GoogleSurveyService.LinkGoogleFormRequest request,
            HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        SurveyResponse response = googleSurveyService.linkGoogleForm(request, authHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google/forms")
    public ResponseEntity<List<GoogleDriveFormItem>> listGoogleForms(
            @RequestParam UUID connectionId,
            @RequestParam(required = false, defaultValue = "50") int pageSize
    ) {
        List<GoogleDriveFormItem> list = googleSurveyService.listAvailableGoogleForms(connectionId, pageSize);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{surveyId}/google/pull-schema")
    public ResponseEntity<Map<String, String>> pullSchema(@PathVariable UUID surveyId) {
        googleSurveyService.pullSchema(surveyId);
        return ResponseEntity.ok(Map.of("message", "Đã kéo cấu trúc câu hỏi từ Google Form thành công"));
    }

    @PostMapping(value = {"/{surveyId}/google/edit-access", "/{surveyId}/google/request-edit-access"})
    public ResponseEntity<GoogleFormAccessService.EditAccessResponse> requestEditAccess(
            @PathVariable UUID surveyId,
            HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        GoogleFormAccessService.EditAccessResponse result = accessService.requestEditAccess(surveyId, authHeader);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{surveyId}/google/sync-responses")
    public ResponseEntity<GoogleResponseSyncService.SyncResult> syncResponses(@PathVariable UUID surveyId) {
        accessGuard.viewResponses(surveyId);
        GoogleResponseSyncService.SyncResult result = syncService.syncResponses(surveyId, true);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{surveyId}/google/sync-status")
    public ResponseEntity<GoogleIntegrationDto> getSyncStatus(@PathVariable UUID surveyId) {
        GoogleIntegrationDto status = googleSurveyService.getSyncStatus(surveyId);
        return ResponseEntity.ok(status);
    }
}
