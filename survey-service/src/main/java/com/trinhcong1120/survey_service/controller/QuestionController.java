package com.trinhcong1120.survey_service.controller;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.question.*;
import com.trinhcong1120.survey_service.repository.QuestionTypeRepository;
import com.trinhcong1120.survey_service.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Questions")
public class QuestionController {

  private final QuestionService questionService;
  private final QuestionTypeRepository questionTypeRepository;

  public QuestionController(
          QuestionService questionService,
          QuestionTypeRepository questionTypeRepository
  ) {
    this.questionService = questionService;
    this.questionTypeRepository = questionTypeRepository;
  }

  @GetMapping("/types")
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all', 'survey_update', 'survey_update_all')")
  public ResponseEntity<List<QuestionTypeResponse>> getTypes() {
    return ResponseEntity.ok(questionTypeRepository.findAll().stream()
            .map(type -> new QuestionTypeResponse(type.getId(), type.getCode(), type.getName()))
            .toList());
  }

  @GetMapping("/page/{pageId}")
  @PreAuthorize("hasAnyAuthority('survey_view', 'survey_view_all', 'survey_update_all')")
  public ResponseEntity<List<QuestionResponse>> getByPage(
          @PathVariable UUID pageId
  ) {
    return ResponseEntity.ok(
            questionService.getByPage(pageId)
    );
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<QuestionResponse> create(
          @Valid @RequestBody CreateQuestionRequest request
  ) {
    return ResponseEntity.ok(
            questionService.create(request)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<QuestionResponse> update(
          @PathVariable UUID id,
          @Valid @RequestBody UpdateQuestionRequest request
  ) {
    return ResponseEntity.ok(
            questionService.update(
                    id,
                    request
            )
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('survey_update', 'survey_update_all')")
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable UUID id
  ) {
    questionService.delete(id);

    return ResponseEntity.ok(
            Map.of(
                    "message",
                    "Đã xóa câu hỏi"
            )
    );
  }
}
