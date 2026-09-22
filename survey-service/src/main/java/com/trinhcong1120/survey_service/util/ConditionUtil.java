package com.trinhcong1120.survey_service.util;

import com.trinhcong1120.survey_service.entity.Answer;
import com.trinhcong1120.survey_service.entity.AnswerOption;
import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.Question;

import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ConditionUtil {

  private static final DateTimeFormatter DATE_FORMATTER =
          DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private ConditionUtil() {
  }

  public static boolean isApplicable(
          Question question,
          Map<Integer, Answer> answerMap,
          List<Condition> conditions
  ) {

    return isApplicable(
            question,
            answerMap,
            conditions,
            new HashMap<>(),
            new HashSet<>()
    );
  }

  public static Map<Integer, Boolean> calculateApplicability(
          List<Question> questions,
          List<Answer> answers,
          List<Condition> conditions
  ) {

    Map<Integer, Answer> answerMap =
            new HashMap<>();

    if (answers != null) {

      for (Answer answer : answers) {

        if (answer == null
                || answer.getQuestion() == null
                || answer.getQuestion().getId() == null) {

          continue;
        }

        answerMap.put(
                answer.getQuestion().getId(),
                answer
        );
      }
    }

    Map<Integer, Boolean> cache =
            new HashMap<>();

    if (questions == null) {
      return cache;
    }

    for (Question question : questions) {

      if (question == null
              || question.getId() == null) {
        continue;
      }

      boolean applicable =
              isApplicable(
                      question,
                      answerMap,
                      conditions,
                      cache,
                      new HashSet<>()
              );

      cache.put(
              question.getId(),
              applicable
      );
    }

    return cache;
  }

  private static boolean isApplicable(
          Question question,
          Map<Integer, Answer> answerMap,
          List<Condition> conditions,
          Map<Integer, Boolean> cache,
          Set<Integer> visiting
  ) {

    if (question == null
            || question.getId() == null) {

      return true;
    }

    Integer questionId =
            question.getId();

    if (cache.containsKey(questionId)) {
      return cache.get(questionId);
    }

    /*
     * Tránh vòng lặp condition:
     *
     * A -> B
     * B -> A
     */
    if (!visiting.add(questionId)) {
      return false;
    }

    List<Condition> targetConditions =
            conditions == null
                    ? List.of()
                    : conditions.stream()
                    .filter(condition ->
                            condition != null
                                    && condition
                                    .getTargetQuestion()
                                    != null
                                    && Objects.equals(
                                    condition
                                            .getTargetQuestion()
                                            .getId(),
                                    questionId
                            ))
                    .toList();

    /*
     * Không có condition target
     * => mặc định hiển thị.
     */
    if (targetConditions.isEmpty()) {

      visiting.remove(questionId);
      cache.put(questionId, true);

      return true;
    }

    boolean hasShowCondition = false;
    boolean matchedShow = false;
    boolean matchedHide = false;

    for (Condition condition :
            targetConditions) {

      Question source =
              condition.getSourceQuestion();

      if (source == null
              || source.getId() == null) {

        continue;
      }

      /*
       * Source không applicable
       * => condition này không thể kích hoạt.
       */
      boolean sourceApplicable =
              isApplicable(
                      source,
                      answerMap,
                      conditions,
                      cache,
                      visiting
              );

      String action =
              normalizeAction(
                      condition.getAction()
              );

      if ("SHOW".equals(action)) {
        hasShowCondition = true;
      }

      if (!sourceApplicable) {
        continue;
      }

      Answer sourceAnswer =
              answerMap.get(
                      source.getId()
              );

      boolean matched =
              matches(
                      condition,
                      source,
                      sourceAnswer
              );

      if (!matched) {
        continue;
      }

      /*
       * HIDE luôn ưu tiên SHOW.
       */
      if ("HIDE".equals(action)) {
        matchedHide = true;
      }

      if ("SHOW".equals(action)) {
        matchedShow = true;
      }
    }

    boolean result;

    if (matchedHide) {

      result = false;

    } else if (hasShowCondition) {

      /*
       * Có SHOW condition:
       * chỉ hiển thị khi ít nhất
       * một SHOW condition match.
       */
      result = matchedShow;

    } else {

      /*
       * Chỉ có HIDE condition
       * nhưng không cái nào match.
       */
      result = true;
    }

    visiting.remove(questionId);

    cache.put(
            questionId,
            result
    );

    return result;
  }

  public static boolean matches(
          Condition condition,
          Question sourceQuestion,
          Answer sourceAnswer
  ) {

    if (condition == null
            || sourceQuestion == null
            || sourceAnswer == null) {

      return false;
    }

    List<String> expectedValues =
            splitValues(
                    condition.getSourceValue()
            );

    if (expectedValues.isEmpty()) {
      return false;
    }

    String type = "";

    if (sourceQuestion.getQuestionType() != null
            && sourceQuestion
            .getQuestionType()
            .getCode() != null) {

      type = sourceQuestion
              .getQuestionType()
              .getCode()
              .trim()
              .toUpperCase();
    }

    return switch (type) {

      case "SINGLE_CHOICE",
           "MULTIPLE_CHOICE" ->
              matchChoice(
                      sourceAnswer,
                      expectedValues
              );

      case "NUMBER" ->
              matchNumber(
                      sourceAnswer,
                      expectedValues
              );

      case "DATE" ->
              matchDate(
                      sourceAnswer,
                      expectedValues
              );

      case "ADDRESS" ->
              matchAddress(
                      sourceAnswer,
                      expectedValues
              );

      default ->
              matchText(
                      sourceAnswer,
                      expectedValues
              );
    };
  }

  private static boolean matchChoice(
          Answer answer,
          List<String> expectedValues
  ) {

    if (answer.getAnswerOptions() == null) {
      return false;
    }

    Set<String> selectedIds =
            new HashSet<>();

    for (AnswerOption answerOption :
            answer.getAnswerOptions()) {

      if (answerOption == null
              || answerOption.getOption() == null
              || answerOption
              .getOption()
              .getId() == null) {

        continue;
      }

      selectedIds.add(
              String.valueOf(
                      answerOption
                              .getOption()
                              .getId()
              )
      );
    }

    for (String expected :
            expectedValues) {

      if (selectedIds.contains(expected)) {
        return true;
      }
    }

    return false;
  }

  private static boolean matchNumber(
          Answer answer,
          List<String> expectedValues
  ) {

    if (answer.getAnswerNumber() == null) {
      return false;
    }

    String actual =
            normalizeNumber(
                    answer.getAnswerNumber()
            );

    for (String expected :
            expectedValues) {

      try {

        Double value =
                Double.parseDouble(expected);

        if (actual.equals(
                normalizeNumber(value)
        )) {
          return true;
        }

      } catch (NumberFormatException ignored) {
      }
    }

    return false;
  }

  private static boolean matchDate(
          Answer answer,
          List<String> expectedValues
  ) {

    if (answer.getAnswerDate() == null) {
      return false;
    }

    String actual =
            answer.getAnswerDate()
                    .format(DATE_FORMATTER);

    for (String expected :
            expectedValues) {

      if (actual.equalsIgnoreCase(
              expected.trim()
      )) {
        return true;
      }
    }

    return false;
  }

  private static boolean matchAddress(
          Answer answer,
          List<String> expectedValues
  ) {

    String province =
            normalize(answer.getProvince());

    String ward =
            normalize(answer.getWard());

    for (String expected :
            expectedValues) {

      String value =
              normalize(expected);

      if (value == null) {
        continue;
      }

      if (province != null
              && province.equalsIgnoreCase(value)) {

        return true;
      }

      if (ward != null
              && ward.equalsIgnoreCase(value)) {

        return true;
      }
    }

    return false;
  }

  private static boolean matchText(
          Answer answer,
          List<String> expectedValues
  ) {

    String actual =
            normalize(answer.getAnswerText());

    if (actual == null) {
      return false;
    }

    for (String expected :
            expectedValues) {

      if (actual.equalsIgnoreCase(
              expected.trim()
      )) {
        return true;
      }
    }

    return false;
  }

  private static List<String> splitValues(
          String sourceValue
  ) {

    if (sourceValue == null
            || sourceValue.isBlank()) {

      return List.of();
    }

    return Arrays.stream(
                    sourceValue.split(",")
            )
            .map(String::trim)
            .filter(value ->
                    !value.isBlank()
            )
            .toList();
  }

  private static String normalizeAction(
          String action
  ) {

    if (action == null) {
      return "";
    }

    return action.trim()
            .toUpperCase();
  }

  private static String normalize(
          String value
  ) {

    if (value == null) {
      return null;
    }

    String result = value.trim();

    return result.isEmpty()
            ? null
            : result;
  }

  private static String normalizeNumber(
          Double value
  ) {

    if (value == null) {
      return null;
    }

    if (value % 1 == 0) {
      return String.valueOf(
              value.longValue()
      );
    }

    return String.valueOf(value);
  }
}