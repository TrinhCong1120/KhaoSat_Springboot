package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.response.ResponseDetailResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseListResponse;
import com.trinhcong1120.survey_service.service.ResponseService;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<List<ResponseListResponse>>
  getBySurvey(
          @PathVariable UUID surveyId
  ) {
    return ResponseEntity.ok(
            responseService.getBySurvey(
                    surveyId
            )
    );
  }

  @PostMapping("/survey/{surveyId}/filter")
  public ResponseEntity<List<ResponseListResponse>>
  filter(
          @PathVariable UUID surveyId,
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
