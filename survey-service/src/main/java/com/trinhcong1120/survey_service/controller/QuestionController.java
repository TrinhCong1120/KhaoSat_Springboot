package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.dto.question.*;
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

  public QuestionController(
          QuestionService questionService
  ) {
    this.questionService = questionService;
  }

  @GetMapping("/page/{pageId}")
  @PreAuthorize("hasAuthority('survey_view')")
  public ResponseEntity<List<QuestionResponse>> getByPage(
          @PathVariable Integer pageId
  ) {
    return ResponseEntity.ok(
            questionService.getByPage(pageId)
    );
  }

  @PostMapping
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<QuestionResponse> create(
          @Valid @RequestBody CreateQuestionRequest request
  ) {
    return ResponseEntity.ok(
            questionService.create(request)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<QuestionResponse> update(
          @PathVariable Integer id,
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
  @PreAuthorize("hasAuthority('survey_update')")
  public ResponseEntity<Map<String, String>> delete(
          @PathVariable Integer id
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
