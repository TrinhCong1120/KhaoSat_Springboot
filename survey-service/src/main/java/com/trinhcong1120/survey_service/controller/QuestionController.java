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

  public ResponseEntity<List<QuestionTypeResponse>> getTypes() {
    return ResponseEntity.ok(questionTypeRepository.findAll().stream()
            .map(type -> new QuestionTypeResponse(type.getId(), type.getCode(), type.getName()))
            .toList());
  }

  @GetMapping("/page/{pageId}")

  public ResponseEntity<List<QuestionResponse>> getByPage(
          @PathVariable UUID pageId
  ) {
    return ResponseEntity.ok(
            questionService.getByPage(pageId)
    );
  }

  @PostMapping

  public ResponseEntity<QuestionResponse> create(
          @Valid @RequestBody CreateQuestionRequest request
  ) {
    return ResponseEntity.ok(
            questionService.create(request)
    );
  }

  @PutMapping("/{id}")

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
