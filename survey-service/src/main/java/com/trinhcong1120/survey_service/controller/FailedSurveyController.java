package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.failed.FailedSurveyResponse;
import com.trinhcong1120.survey_service.dto.failed.ImportFailedSurveyResponse;
import com.trinhcong1120.survey_service.service.FailedSurveyCaseService;
import com.trinhcong1120.survey_service.service.FailedSurveyCaseService.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/FailedSurveys")
@PreAuthorize("hasAuthority('survey_failed_manage')")
public class FailedSurveyController {
  private final FailedSurveyCaseService cases;

  public FailedSurveyController(FailedSurveyCaseService cases) { this.cases = cases; }

  @GetMapping("/cases")
  public CasePage listCases(@RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String search) {
    return cases.list(page, pageSize, status, search);
  }

  @GetMapping("/cases/assignees")
  public List<Assignee> assignees() { return cases.assignees(); }

  @GetMapping("/cases/{id}")
  public CaseDetail detail(@PathVariable UUID id) { return cases.detail(id); }

  @PostMapping(value = "/cases", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CaseView upload(@RequestParam("file") MultipartFile file) { return cases.upload(file); }

  @PatchMapping("/cases/{id}")
  public CaseDetail update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
    return cases.update(id, request);
  }

  @PostMapping("/cases/{id}/import")
  public ImportFailedSurveyResponse importCase(@PathVariable UUID id) { return cases.importCase(id); }

  @GetMapping("/cases/{id}/download")
  public ResponseEntity<byte[]> downloadCase(@PathVariable UUID id) {
    CaseView item = cases.detail(id).item();
    byte[] data = cases.download(id);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + item.fileName().replace("\"", "") + "\"")
        .contentType(MediaType.APPLICATION_JSON).contentLength(data.length).body(data);
  }

  @DeleteMapping("/cases/{id}")
  public ResponseEntity<Void> deleteCase(@PathVariable UUID id) {
    cases.delete(id);
    return ResponseEntity.noContent().build();
  }

  // Old URLs remain available to existing clients and use the same case store and guard.
  @GetMapping
  public List<FailedSurveyResponse> getFiles(@RequestParam(defaultValue = "false") boolean includeContent) {
    return cases.legacyList(includeContent);
  }

  @GetMapping("/{fileName}/download")
  public ResponseEntity<byte[]> download(@PathVariable String fileName) {
    return downloadCase(cases.idByFileName(fileName));
  }

  @DeleteMapping("/{fileName}")
  public Map<String, String> deleteOne(@PathVariable String fileName) {
    cases.delete(cases.idByFileName(fileName));
    return Map.of("fileName", fileName);
  }

  @DeleteMapping
  public Map<String, Object> deleteAll() {
    List<String> names = cases.legacyList(false).stream().map(FailedSurveyResponse::getFileName).toList();
    for (String name : names) cases.delete(cases.idByFileName(name));
    return Map.of("count", names.size(), "files", names);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportFailedSurveyResponse importFile(@RequestParam("file") MultipartFile file) {
    return cases.importCase(cases.upload(file).id());
  }
}
