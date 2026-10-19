package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.condition.*;
import com.trinhcong1120.survey_service.service.ConditionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Conditions")
public class ConditionController {

  private final ConditionService conditionService;

  public ConditionController(
          ConditionService conditionService
  ) {
    this.conditionService = conditionService;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all')")
  public ResponseEntity<List<ConditionResponse>> getAll() {
    return ResponseEntity.ok(
            conditionService.getAll()
    );
  }

  @GetMapping("/survey/{surveyId}")
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all')")
  public ResponseEntity<List<ConditionResponse>> getBySurvey(
          @PathVariable UUID surveyId
  ) {
    return ResponseEntity.ok(
            conditionService.getBySurvey(
                    surveyId
            )
    );
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<ConditionResponse> create(
          @Valid @RequestBody CreateConditionRequest request
  ) {
    return ResponseEntity.ok(
            conditionService.create(request)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<ConditionResponse> update(
          @PathVariable UUID id,
          @Valid @RequestBody UpdateConditionRequest request
  ) {
    return ResponseEntity.ok(
            conditionService.update(
                    id,
                    request
            )
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable UUID id
  ) {
    conditionService.delete(id);

    return ResponseEntity.ok(
            Map.of(
                    "message",
                    "Đã xóa condition"
            )
    );
  }
}
