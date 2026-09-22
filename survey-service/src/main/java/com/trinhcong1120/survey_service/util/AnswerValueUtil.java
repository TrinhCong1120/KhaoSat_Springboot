package com.trinhcong1120.survey_service.util;

import com.trinhcong1120.survey_service.entity.Answer;
import com.trinhcong1120.survey_service.entity.AnswerOption;
import com.trinhcong1120.survey_service.entity.Question;

import java.time.format.DateTimeFormatter;
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

    Double value =
            answer.getAnswerNumber();

    if (value % 1 == 0) {
      return String.valueOf(
              value.longValue()
      );
    }

    return String.valueOf(value);
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

    if (province == null && ward == null) {
      return null;
    }

    if (province == null) {
      return ward;
    }

    if (ward == null) {
      return province;
    }

    return province + " / " + ward;
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
              normalize(answer.getProvince()) != null
                      && normalize(answer.getWard()) != null;

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