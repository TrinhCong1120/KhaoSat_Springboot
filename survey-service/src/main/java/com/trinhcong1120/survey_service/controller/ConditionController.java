package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.condition.*;
import com.trinhcong1120.survey_service.service.ConditionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<List<ConditionResponse>> getAll() {
    return ResponseEntity.ok(
            conditionService.getAll()
    );
  }

  @GetMapping("/survey/{surveyId}")
  public ResponseEntity<List<ConditionResponse>> getBySurvey(
          @PathVariable Integer surveyId
  ) {
    return ResponseEntity.ok(
            conditionService.getBySurvey(
                    surveyId
            )
    );
  }

  @PostMapping
  public ResponseEntity<ConditionResponse> create(
          @Valid @RequestBody CreateConditionRequest request
  ) {
    return ResponseEntity.ok(
            conditionService.create(request)
    );
  }

  @PutMapping("/{id}")
  public ResponseEntity<ConditionResponse> update(
          @PathVariable Integer id,
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
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable Integer id
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
