package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.client.CoreUserClient;
import com.trinhcong1120.survey_service.dto.core.CoreUserResponse;
import com.trinhcong1120.survey_service.dto.survey.*;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.service.SurveyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Surveys")
public class SurveyController {

  private final SurveyService surveyService;
  private final CoreUserClient coreUserClient;

  public SurveyController(
          SurveyService surveyService,
          CoreUserClient coreUserClient
  ) {
    this.surveyService = surveyService;
    this.coreUserClient = coreUserClient;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<List<SurveyResponse>> getAll() {
    return ResponseEntity.ok(
            surveyService.getAll()
    );
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<SurveyDetailResponse> getDetail(
          @PathVariable Integer id
  ) {
    return ResponseEntity.ok(
            surveyService.getDetail(id)
    );
  }

  @PostMapping
  @PreAuthorize("hasAuthority('survey_create')")
  public ResponseEntity<SurveyResponse> create(
          @Valid @RequestBody CreateSurveyRequest request,
          HttpServletRequest httpRequest
  ) {
    String authorization =
            httpRequest.getHeader("Authorization");

    CoreUserResponse user =
            coreUserClient.getMe(authorization);

    String username =
            user.getUsername();

    if (username == null
            || username.isBlank()) {
      throw new BadRequestException(
              "Khong lay duoc username tu core-service");
    }

    return ResponseEntity.ok(
            surveyService.create(
                    request,
                    username
            )
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<SurveyResponse> update(
          @PathVariable Integer id,
          @Valid @RequestBody UpdateSurveyRequest request
  ) {
    return ResponseEntity.ok(
            surveyService.update(id, request)
    );
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<SurveyResponse> updateStatus(
          @PathVariable Integer id,
          @Valid @RequestBody UpdateSurveyStatusRequest request
  ) {
    return ResponseEntity.ok(
            surveyService.updateStatus(
                    id,
                    request
            )
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('survey_delete')")
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable Integer id
  ) {
    surveyService.delete(id);

    return ResponseEntity.ok(
            Map.of(
                    "message",
                    "Đã xóa"
            )
    );
  }
}
