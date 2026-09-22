package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.response.ResponseDetailResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseListResponse;
import com.trinhcong1120.survey_service.service.ResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/Responses")
public class ResponseController {

  private final ResponseService responseService;

  public ResponseController(
          ResponseService responseService
  ) {
    this.responseService = responseService;
  }

  @GetMapping("/survey/{surveyId}")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<List<ResponseListResponse>>
  getBySurvey(
          @PathVariable Integer surveyId
  ) {
    return ResponseEntity.ok(
            responseService.getBySurvey(
                    surveyId
            )
    );
  }

  @PostMapping("/survey/{surveyId}/filter")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<List<ResponseListResponse>>
  filter(
          @PathVariable Integer surveyId,
          @RequestBody ResponseFilterRequest request
  ) {
    return ResponseEntity.ok(
            responseService.filter(
                    surveyId,
                    request
            )
    );
  }

  @GetMapping("/{responseId}")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<ResponseDetailResponse> getDetail(
          @PathVariable Integer responseId
  ) {
    return ResponseEntity.ok(
            responseService.getDetail(
                    responseId
            )
    );
  }
}
