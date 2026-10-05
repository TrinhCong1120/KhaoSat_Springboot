package com.trinhcong1120.survey_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(
          NotFoundException ex
  ) {
    return buildResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
    );
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(
          BadRequestException ex
  ) {
    return buildResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
    );
  }

  @ExceptionHandler(AnswerValidationException.class)
  public ResponseEntity<Map<String, Object>> handleAnswerValidation(
          AnswerValidationException ex
  ) {

    Map<String, Object> body = new LinkedHashMap<>();

    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    body.put("message", ex.getMessage());
    body.put("fieldErrors", ex.getFieldErrors());

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
          MethodArgumentNotValidException ex
  ) {

    String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Dữ liệu không hợp lệ");

    return buildResponse(
            HttpStatus.BAD_REQUEST,
            message
    );
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(
          ResponseStatusException ex
  ) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

    return buildResponse(
            status,
            ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
          AccessDeniedException ex
  ) {
    return buildResponse(
            HttpStatus.FORBIDDEN,
            "Bạn không có quyền thực hiện chức năng này"
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(
          Exception ex
  ) {
    return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Đã xảy ra lỗi hệ thống"
    );
  }

  private ResponseEntity<Map<String, Object>> buildResponse(
          HttpStatus status,
          String message
  ) {

    Map<String, Object> body = new LinkedHashMap<>();

    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);

    return ResponseEntity
            .status(status)
            .body(body);
  }
}
