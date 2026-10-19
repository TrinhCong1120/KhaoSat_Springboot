package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.common.PagedResponse;
import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.response.ResponseDetailResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseListResponse;
import com.trinhcong1120.survey_service.service.ResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/Responses")
@PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all')")
@Validated
public class ResponseController {

  private final ResponseService responseService;

  public ResponseController(
          ResponseService responseService
  ) {
    this.responseService = responseService;
  }

  @GetMapping("/survey/{surveyId}")
  public ResponseEntity<PagedResponse<ResponseListResponse>>
  getBySurvey(
          @PathVariable UUID surveyId,
          @RequestParam(defaultValue = "1") @Min(1) int page,
          @RequestParam(required = false) String q
  ) {
    return ResponseEntity.ok(
            responseService.getBySurvey(
                    surveyId,
                    page,
                    q
            )
    );
  }

  @PostMapping("/survey/{surveyId}/filter")
  public ResponseEntity<PagedResponse<ResponseListResponse>>
  filter(
          @PathVariable UUID surveyId,
          @RequestParam(defaultValue = "1") @Min(1) int page,
          @RequestParam(required = false) String q,
          @RequestBody ResponseFilterRequest request
  ) {
    return ResponseEntity.ok(
            responseService.filter(
                    surveyId,
                    request,
                    page,
                    q
            )
    );
  }

  @GetMapping("/{responseId}")
  public ResponseEntity<ResponseDetailResponse> getDetail(
          @PathVariable UUID responseId
  ) {
    return ResponseEntity.ok(
            responseService.getDetail(
                    responseId
            )
    );
  }
}
