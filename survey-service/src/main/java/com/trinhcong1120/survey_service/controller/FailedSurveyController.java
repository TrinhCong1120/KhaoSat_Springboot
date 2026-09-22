package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.failed.FailedSurveyResponse;
import com.trinhcong1120.survey_service.dto.failed.ImportFailedSurveyResponse;
import com.trinhcong1120.survey_service.service.FailedSurveyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/FailedSurveys")
@PreAuthorize("hasAuthority('survey_update')")
public class FailedSurveyController {

  private final FailedSurveyService failedSurveyService;

  public FailedSurveyController(
          FailedSurveyService failedSurveyService
  ) {
    this.failedSurveyService = failedSurveyService;
  }

  @GetMapping
  public ResponseEntity<List<FailedSurveyResponse>> getFiles(
          @RequestParam(
                  defaultValue = "false"
          ) boolean includeContent
  ) {
    return ResponseEntity.ok(
            failedSurveyService.getFiles(
                    includeContent
            )
    );
  }

  @GetMapping("/{fileName}/download")
  public ResponseEntity<byte[]> download(
          @PathVariable String fileName
  ) {

    byte[] data =
            failedSurveyService.download(
                    fileName
            );

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\""
                            + fileName
                            + "\""
            )
            .contentType(
                    MediaType.APPLICATION_JSON
            )
            .contentLength(data.length)
            .body(data);
  }

  @DeleteMapping("/{fileName}")
  public ResponseEntity<Map<String, String>> deleteOne(
          @PathVariable String fileName
  ) {

    String deleted =
            failedSurveyService.deleteOne(
                    fileName
            );

    return ResponseEntity.ok(
            Map.of(
                    "fileName",
                    deleted
            )
    );
  }

  @DeleteMapping
  public ResponseEntity<Map<String, Object>> deleteAll() {

    List<String> deleted =
            failedSurveyService.deleteAll();

    return ResponseEntity.ok(
            Map.of(
                    "count",
                    deleted.size(),
                    "files",
                    deleted
            )
    );
  }

  @PostMapping(
          value = "/import",
          consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ResponseEntity<ImportFailedSurveyResponse> importFile(
          @RequestParam("file") MultipartFile file
  ) {

    return ResponseEntity.ok(
            failedSurveyService.importFile(
                    file
            )
    );
  }
}