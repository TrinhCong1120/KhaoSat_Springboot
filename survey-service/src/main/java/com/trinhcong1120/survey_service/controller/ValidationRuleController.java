package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.validation.QuestionValidationRuleResponse;
import com.trinhcong1120.survey_service.dto.validation.UpdateQuestionValidationRulesRequest;
import com.trinhcong1120.survey_service.dto.validation.ValidationRuleCatalogResponse;
import com.trinhcong1120.survey_service.service.QuestionValidationRuleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ValidationRuleController {

  private final QuestionValidationRuleService questionValidationRuleService;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;

  public ValidationRuleController(
          QuestionValidationRuleService questionValidationRuleService,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard
  ) {
    this.questionValidationRuleService = questionValidationRuleService;
    this.guard = guard;
  }

  @GetMapping("/api/ValidationRules/catalog")
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all')")
  public ResponseEntity<ValidationRuleCatalogResponse> catalog(
          @RequestParam(required = false) String questionType
  ) {
    return ResponseEntity.ok(
            questionValidationRuleService.getCatalog(questionType)
    );
  }

  @GetMapping("/api/Questions/{id}/validation-rules")
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all')")
  public ResponseEntity<List<QuestionValidationRuleResponse>> getRules(
          @PathVariable UUID id
  ) {
    guard.viewQuestion(id);
    return ResponseEntity.ok(
            questionValidationRuleService.getByQuestion(id)
    );
  }

  @PutMapping("/api/Questions/{id}/validation-rules")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<List<QuestionValidationRuleResponse>> replaceRules(
          @PathVariable UUID id,
          @Valid @RequestBody UpdateQuestionValidationRulesRequest request
  ) {
    guard.editQuestion(id);
    return ResponseEntity.ok(
            questionValidationRuleService.replaceRules(id, request)
    );
  }

  @PostMapping("/api/Questions/{id}/validation-rules/validate-definition")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<Map<String, Object>> validateDefinition(
          @PathVariable UUID id,
          @Valid @RequestBody UpdateQuestionValidationRulesRequest request
  ) {
    guard.editQuestion(id);
    questionValidationRuleService.validateDraft(id, request);

    return ResponseEntity.ok(
            Map.of("valid", true)
    );
  }

  @PostMapping("/api/Questions/{id}/validation-rules/test")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<Map<String, Object>> testDefinition(
          @PathVariable UUID id,
          @Valid @RequestBody UpdateQuestionValidationRulesRequest request
  ) {
    guard.editQuestion(id);
    questionValidationRuleService.validateDraft(id, request);

    return ResponseEntity.ok(
            Map.of("valid", true)
    );
  }
}
