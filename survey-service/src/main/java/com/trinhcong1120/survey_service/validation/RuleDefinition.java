package com.trinhcong1120.survey_service.validation;

import java.util.List;
import java.util.Set;

public record RuleDefinition(
        String code,
        String name,
        String description,
        Set<String> applicableQuestionTypes,
        List<ParameterSpec> parameters,
        String editor,
        String defaultMessage
) {
  public boolean appliesTo(String questionType) {
    return applicableQuestionTypes.contains(questionType);
  }
}
