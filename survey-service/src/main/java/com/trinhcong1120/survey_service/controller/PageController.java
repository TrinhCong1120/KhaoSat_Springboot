package com.trinhcong1120.survey_service.controller;

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
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<List<PageResponse>> getBySurvey(
          @PathVariable Integer surveyId
  ) {
    return ResponseEntity.ok(
            pageService.getBySurvey(surveyId)
    );
  }

  @PostMapping
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<PageResponse> create(
          @Valid @RequestBody CreatePageRequest request
  ) {
    return ResponseEntity.ok(
            pageService.create(request)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<PageResponse> update(
          @PathVariable Integer id,
          @Valid @RequestBody UpdatePageRequest request
  ) {
    return ResponseEntity.ok(
            pageService.update(id, request)
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable Integer id
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
