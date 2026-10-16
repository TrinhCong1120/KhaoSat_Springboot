package com.trinhcong1120.survey_service.exception;

import com.trinhcong1120.survey_service.dto.submit.AnswerValidationError;
import java.util.List;

public class AnswerValidationException extends RuntimeException {

  private final List<AnswerValidationError> fieldErrors;

  public AnswerValidationException(List<AnswerValidationError> fieldErrors) {
    super("Du lieu cau tra loi khong hop le");
    this.fieldErrors = fieldErrors;
  }

  public List<AnswerValidationError> getFieldErrors() {
    return fieldErrors;
  }
}
