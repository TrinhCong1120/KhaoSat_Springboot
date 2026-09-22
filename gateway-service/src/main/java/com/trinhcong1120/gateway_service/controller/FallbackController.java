package com.trinhcong1120.gateway_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> fallback() {
    Map<String, Object> body = new LinkedHashMap<>();

    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
    body.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
    body.put("message", "Service dang tam thoi khong kha dung");

    return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(body);
  }
}
