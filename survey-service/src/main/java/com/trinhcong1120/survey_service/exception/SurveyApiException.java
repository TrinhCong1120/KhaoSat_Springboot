package com.trinhcong1120.survey_service.exception;

import org.springframework.http.HttpStatus;

public class SurveyApiException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public SurveyApiException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() { return code; }
  public HttpStatus getStatus() { return status; }
}
