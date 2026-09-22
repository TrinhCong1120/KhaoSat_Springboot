package com.trinhcong1120.gateway_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.support.NotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log =
          LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ==========================================
  // 404 - RESOURCE NOT FOUND
  // ==========================================

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNoResource(
          NoResourceFoundException ex,
          ServerWebExchange exchange
  ) {

    log.warn(
            "Resource not found: {} {}",
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath().value(),
            ex
    );

    return buildResponse(
            HttpStatus.NOT_FOUND,
            "Khong tim thay duong dan",
            exchange
    );
  }

  // ==========================================
  // 503 - SERVICE NOT FOUND IN EUREKA
  // ==========================================

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleServiceNotFound(
          NotFoundException ex,
          ServerWebExchange exchange
  ) {

    log.error(
            "Gateway service not found: {} {}",
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath().value(),
            ex
    );

    return buildResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Khong tim thay service trong Eureka",
            exchange
    );
  }

  // ==========================================
  // HTTP STATUS EXCEPTIONS
  // ==========================================

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(
          ResponseStatusException ex,
          ServerWebExchange exchange
  ) {

    log.error(
            "Gateway HTTP error: {} {}",
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath().value(),
            ex
    );

    return buildResponse(
            ex.getStatusCode(),
            ex.getReason() != null
                    ? ex.getReason()
                    : "Gateway request failed",
            exchange
    );
  }

  // ==========================================
  // ALL OTHER EXCEPTIONS
  // ==========================================

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(
          Exception ex,
          ServerWebExchange exchange
  ) {

    log.error(
            "Gateway request failed: {} {}",
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath().value(),
            ex
    );

    return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Da xay ra loi gateway",
            exchange
    );
  }

  // ==========================================
  // BUILD RESPONSE
  // ==========================================

  private ResponseEntity<Map<String, Object>> buildResponse(
          HttpStatusCode status,
          String message,
          ServerWebExchange exchange
  ) {

    Map<String, Object> body = new LinkedHashMap<>();

    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());

    HttpStatus httpStatus = HttpStatus.resolve(status.value());

    body.put(
            "error",
            httpStatus != null
                    ? httpStatus.getReasonPhrase()
                    : "HTTP Error"
    );

    body.put("message", message);

    body.put(
            "path",
            exchange.getRequest().getPath().value()
    );

    return ResponseEntity
            .status(status)
            .body(body);
  }
}