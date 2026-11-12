package com.trinhcong1120.survey_service.util;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.Answer;
import com.trinhcong1120.survey_service.entity.AnswerOption;
import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Question;

import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.*;

public final class ConditionUtil {

  public static Set<UUID> unreachableShowTargets(List<Condition> conditions, Set<UUID> openPageIds) {
    List<Condition> shows = conditions.stream()
        .filter(c -> c.getSourceQuestion() != null && (c.getTargetQuestion() != null || c.getTargetPage() != null))
        .filter(c -> "SHOW".equalsIgnoreCase(c.getAction())).toList();
    Set<UUID> excluded = new HashSet<>();
    boolean changed;
    do {
      changed = false;
      for (UUID targetId : shows.stream().filter(c -> c.getTargetQuestion() != null).map(c -> c.getTargetQuestion().getId()).distinct().toList()) {
        List<Condition> targetShows = shows.stream()
            .filter(c -> c.getTargetQuestion() != null && targetId.equals(c.getTargetQuestion().getId())).toList();
        if (targetShows.stream().allMatch(c -> c.getSourceQuestion().getPage() == null || !openPageIds.contains(c.getSourceQuestion().getPage().getId())
            || excluded.contains(c.getSourceQuestion().getId())))
          changed |= excluded.add(targetId);
      }
    } while (changed);
    return excluded;
  }

  private static final DateTimeFormatter DATE_FORMATTER =
          DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private ConditionUtil() {
  }

  public static boolean isApplicable(
          Question question,
          Map<UUID, Answer> answerMap,
          List<Condition> conditions
  ) {
    return isApplicable(
            question,
            answerMap,
            conditions,
            new HashMap<>(),
            new HashMap<>(),
            new HashSet<>(),
            new HashSet<>()
    );
  }

  public static boolean isPageApplicable(
          Page page,
          Map<UUID, Answer> answerMap,
          List<Condition> conditions
  ) {
    return isPageApplicable(
            page,
            answerMap,
            conditions,
            new HashMap<>(),
            new HashMap<>(),
            new HashSet<>()
    );
  }

  public static Map<UUID, Boolean> calculateApplicability(
          List<Question> questions,
          List<Answer> answers,
          List<Condition> conditions
  ) {

    Map<UUID, Answer> answerMap =
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

    Map<UUID, Boolean> questionCache = new HashMap<>();
    Map<UUID, Boolean> pageCache = new HashMap<>();

    if (questions == null) {
      return questionCache;
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
                      questionCache,
                      pageCache,
                      new HashSet<>(),
                      new HashSet<>()
              );

      questionCache.put(
              question.getId(),
              applicable
      );
    }

    return questionCache;
  }

  public static boolean isPageApplicable(
          Page page,
          Map<UUID, Answer> answerMap,
          List<Condition> conditions,
          Map<UUID, Boolean> pageCache,
          Map<UUID, Boolean> questionCache,
          Set<UUID> visiting
  ) {
    if (page == null || page.getId() == null) {
      return true;
    }
    UUID pageId = page.getId();
    if (pageCache.containsKey(pageId)) {
      return pageCache.get(pageId);
    }
    if (!visiting.add(pageId)) {
      return false;
    }

    List<Condition> targetConditions = conditions == null
        ? List.of()
        : conditions.stream()
            .filter(c -> c != null && c.getTargetPage() != null && Objects.equals(c.getTargetPage().getId(), pageId))
            .toList();

    if (targetConditions.isEmpty()) {
      visiting.remove(pageId);
      pageCache.put(pageId, true);
      return true;
    }

    boolean hasShowCondition = false;
    boolean matchedShow = false;
    boolean matchedHide = false;

    for (Condition condition : targetConditions) {
      Question source = condition.getSourceQuestion();
      if (source == null || source.getId() == null) {
        continue;
      }

      boolean sourceApplicable = isApplicable(
              source,
              answerMap,
              conditions,
              questionCache,
              pageCache,
              new HashSet<>(),
              new HashSet<>()
      );

      String action = normalizeAction(condition.getAction());
      if ("SHOW".equals(action)) {
        hasShowCondition = true;
      }

      if (!sourceApplicable) {
        continue;
      }

      Answer sourceAnswer = answerMap.get(source.getId());
      boolean matched = matches(condition, source, sourceAnswer);

      if (!matched) {
        continue;
      }

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
      result = matchedShow;
    } else {
      result = true;
    }

    visiting.remove(pageId);
    pageCache.put(pageId, result);
    return result;
  }

  private static boolean isApplicable(
          Question question,
          Map<UUID, Answer> answerMap,
          List<Condition> conditions,
          Map<UUID, Boolean> questionCache,
          Map<UUID, Boolean> pageCache,
          Set<UUID> questionVisiting,
          Set<UUID> pageVisiting
  ) {

    if (question == null
            || question.getId() == null) {
      return true;
    }

    UUID questionId = question.getId();

    if (questionCache.containsKey(questionId)) {
      return questionCache.get(questionId);
    }

    // 1. Kiểm tra điều kiện Trang trước: Nếu trang bị ẩn, toàn bộ câu hỏi trong trang tự động không khả dụng
    if (question.getPage() != null && !isPageApplicable(question.getPage(), answerMap, conditions, pageCache, questionCache, pageVisiting)) {
      questionCache.put(questionId, false);
      return false;
    }

    /*
     * Tránh vòng lặp condition:
     *
     * A -> B
     * B -> A
     */
    if (!questionVisiting.add(questionId)) {
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
      questionVisiting.remove(questionId);
      questionCache.put(questionId, true);
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
                      questionCache,
                      pageCache,
                      questionVisiting,
                      pageVisiting
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
      result = true;
    }

    questionVisiting.remove(questionId);
    questionCache.put(questionId, result);

    return result;
  }

  private static boolean matches(
          Condition condition,
          Question source,
          Answer sourceAnswer
  ) {

    if (sourceAnswer == null) {
      return false;
    }

    List<String> expectedValues =
            splitValues(
                    condition.getSourceValue()
            );

    if (expectedValues.isEmpty()) {
      return false;
    }

    String typeCode =
            source.getQuestionType() == null
                    ? ""
                    : source.getQuestionType()
                    .getCode();

    return switch (typeCode) {

      case "SINGLE_CHOICE", "MULTIPLE_CHOICE" ->
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
        BigDecimal value =
                new BigDecimal(expected);

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
          BigDecimal value
  ) {

    if (value == null) {
      return null;
    }

    return value.stripTrailingZeros().toPlainString();
  }
}
