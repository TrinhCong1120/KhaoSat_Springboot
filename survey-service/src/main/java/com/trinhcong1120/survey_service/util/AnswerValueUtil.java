package com.trinhcong1120.survey_service.util;

import com.trinhcong1120.survey_service.entity.Answer;
import com.trinhcong1120.survey_service.entity.AnswerOption;
import com.trinhcong1120.survey_service.entity.Question;

import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class AnswerValueUtil {

  private static final DateTimeFormatter DATE_FORMATTER =
          DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private AnswerValueUtil() {
  }

  public static String formatAnswer(Answer answer) {

    if (answer == null) {
      return null;
    }

    Question question = answer.getQuestion();

    String type = "";

    if (question != null
            && question.getQuestionType() != null
            && question.getQuestionType().getCode() != null) {

      type = question.getQuestionType()
              .getCode()
              .trim()
              .toUpperCase();
    }

    return switch (type) {

      case "SINGLE_CHOICE",
           "MULTIPLE_CHOICE" ->
              formatChoice(answer);

      case "NUMBER" ->
              formatNumber(answer);

      case "DATE" ->
              formatDate(answer);

      case "ADDRESS" ->
              formatAddress(answer);

      default ->
              formatText(answer);
    };
  }

  public static String formatChoice(Answer answer) {

    if (answer == null
            || answer.getAnswerOptions() == null
            || answer.getAnswerOptions().isEmpty()) {

      return null;
    }

    List<String> values =
            answer.getAnswerOptions()
                    .stream()
                    .map(AnswerOption::getOption)
                    .filter(Objects::nonNull)
                    .map(option ->
                            option.getOptionText()
                    )
                    .filter(Objects::nonNull)
                    .filter(text ->
                            !text.isBlank()
                    )
                    .toList();

    if (values.isEmpty()) {
      return null;
    }

    return String.join(", ", values);
  }

  public static String formatNumber(Answer answer) {

    if (answer == null
            || answer.getAnswerNumber() == null) {

      return null;
    }

    BigDecimal value =
            answer.getAnswerNumber();

    return value.stripTrailingZeros()
            .toPlainString();
  }

  public static String formatDate(Answer answer) {

    if (answer == null
            || answer.getAnswerDate() == null) {

      return null;
    }

    return answer.getAnswerDate()
            .format(DATE_FORMATTER);
  }

  public static String formatAddress(Answer answer) {

    if (answer == null) {
      return null;
    }

    String province =
            normalize(answer.getProvince());

    String ward =
            normalize(answer.getWard());

    String detail =
            normalize(answer.getAddressDetail());

    if (province == null && ward == null && detail == null) {
      return null;
    }

    List<String> parts =
            java.util.stream.Stream.of(detail, ward, province)
                    .filter(Objects::nonNull)
                    .toList();

    return String.join(" / ", parts);
  }

  public static boolean hasAddressValue(Answer answer) {
    if (answer == null) {
      return false;
    }

    return normalize(answer.getProvince()) != null
            || normalize(answer.getWard()) != null
            || normalize(answer.getAddressDetail()) != null;
  }

  public static String formatText(Answer answer) {

    if (answer == null) {
      return null;
    }

    String value =
            normalize(answer.getAnswerText());

    return value;
  }

  public static boolean hasValue(
          Question question,
          Answer answer
  ) {

    if (question == null || answer == null) {
      return false;
    }

    String type = "";

    if (question.getQuestionType() != null
            && question.getQuestionType().getCode() != null) {

      type = question.getQuestionType()
              .getCode()
              .trim()
              .toUpperCase();
    }

    return switch (type) {

      case "SINGLE_CHOICE",
           "MULTIPLE_CHOICE" ->
              answer.getAnswerOptions() != null
                      && !answer.getAnswerOptions()
                      .isEmpty();

      case "NUMBER" ->
              answer.getAnswerNumber() != null;

      case "DATE" ->
              answer.getAnswerDate() != null;

      case "ADDRESS" ->
              hasAddressValue(answer);

      default ->
              normalize(answer.getAnswerText()) != null;
    };
  }

  private static String normalize(String value) {

    if (value == null) {
      return null;
    }

    String result = value.trim();

    if (result.isEmpty()) {
      return null;
    }

    return result;
  }
}
