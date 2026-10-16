package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.page.*;
import com.trinhcong1120.survey_service.service.PageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Pages")
public class PageController {

  private final PageService pageService;

  public PageController(PageService pageService) {
    this.pageService = pageService;
  }

  @GetMapping("/survey/{surveyId}")
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all', 'survey_update_all')")
  public ResponseEntity<List<PageResponse>> getBySurvey(
          @PathVariable UUID surveyId
  ) {
    return ResponseEntity.ok(
            pageService.getBySurvey(surveyId)
    );
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<PageResponse> create(
          @Valid @RequestBody CreatePageRequest request
  ) {
    return ResponseEntity.ok(
            pageService.create(request)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<PageResponse> update(
          @PathVariable UUID id,
          @Valid @RequestBody UpdatePageRequest request
  ) {
    return ResponseEntity.ok(
            pageService.update(id, request)
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable UUID id
  ) {
    pageService.delete(id);

    return ResponseEntity.ok(
            Map.of(
                    "message",
                    "Đã xóa page"
            )
    );
  }
}
