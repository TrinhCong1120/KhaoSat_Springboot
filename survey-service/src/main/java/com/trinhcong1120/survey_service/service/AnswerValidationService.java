package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.submit.AnswerValidationError;
import com.trinhcong1120.survey_service.dto.submit.SubmitAnswerRequest;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.AnswerValidationException;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.ConditionRepository;
import com.trinhcong1120.survey_service.repository.OptionRepository;
import com.trinhcong1120.survey_service.repository.QuestionRepository;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import com.trinhcong1120.survey_service.validation.RuleDefinition;
import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;

@Service
public class AnswerValidationService {

  private static final Pattern EMAIL_PATTERN =
          Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final ConditionRepository conditionRepository;
  private final QuestionValidationRuleService ruleService;
  private final Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

  public AnswerValidationService(
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ConditionRepository conditionRepository,
          QuestionValidationRuleService ruleService
  ) {
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.conditionRepository = conditionRepository;
    this.ruleService = ruleService;
  }

  public List<SubmitAnswerRequest> validateAndFilter(
          UUID surveyId,
          List<SubmitAnswerRequest> answers
  ) {
    return validate(surveyId, answers, true).visibleAnswers();
  }

  public List<SubmitAnswerRequest> validateAndFilter(UUID surveyId,
          List<SubmitAnswerRequest> answers, Set<UUID> openPageIds) {
    return validate(surveyId, answers, null, true, openPageIds).visibleAnswers();
  }

  public ValidationResult validate(
          UUID surveyId,
          List<SubmitAnswerRequest> answers,
          boolean throwOnError
  ) {
    return validate(surveyId, answers, null, throwOnError);
  }

  public ValidationResult validateSubmittedOnly(
          UUID surveyId,
          List<SubmitAnswerRequest> answers,
          boolean throwOnError
  ) {
    return validateSubmittedOnly(surveyId, answers, throwOnError, null);
  }

  public ValidationResult validateSubmittedOnly(UUID surveyId,
          List<SubmitAnswerRequest> answers, boolean throwOnError, Set<UUID> openPageIds) {
    List<Question> questions = openQuestions(surveyId, openPageIds);
    Map<UUID, Question> questionMap = new HashMap<>();

    for (Question question : questions) {
      questionMap.put(question.getId(), question);
    }

    Map<UUID, SubmitAnswerRequest> answerMap =
            validateSubmittedAnswerShape(surveyId, answers, questionMap);

    List<Condition> conditions =
            conditionRepository
                    .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                            surveyId,
                            surveyId
                    );

    Map<UUID, Boolean> applicability =
            ConditionUtil.calculateApplicability(
                    questions,
                    toConditionAnswers(answerMap),
                    conditions
            );

    Map<UUID, List<QuestionValidationRule>> rulesByQuestion = rulesByQuestion(surveyId);
    List<AnswerValidationError> errors = new ArrayList<>();
    List<SubmitAnswerRequest> visibleAnswers = new ArrayList<>();

    for (Question question : questions) {
      SubmitAnswerRequest answer = answerMap.get(question.getId());

      if (answer == null) {
        continue;
      }

      boolean visible = applicability.getOrDefault(question.getId(), true);

      if (!visible) {
        continue;
      }

      if (Boolean.TRUE.equals(question.getIsRequired()) && !hasValue(question, answer)) {
        errors.add(
                new AnswerValidationError(
                        question.getId(),
                        "REQUIRED",
                        answerField(question),
                        "Cau hoi bat buoc chua duoc tra loi"
                )
        );
        continue;
      }

      if (!hasValue(question, answer)) {
        continue;
      }

      visibleAnswers.add(answer);
      validateRules(
              question,
              answer,
              rulesByQuestion.getOrDefault(question.getId(), List.of()),
              errors
      );
    }

    if (!errors.isEmpty() && throwOnError) {
      throw new AnswerValidationException(errors);
    }

    return new ValidationResult(errors.isEmpty(), visibleAnswers, errors, applicability);
  }

  private ValidationResult validate(
          UUID surveyId,
          List<SubmitAnswerRequest> answers,
          UUID targetPageId,
          boolean throwOnError
  ) {
    return validate(surveyId, answers, targetPageId, throwOnError, null);
  }

  private ValidationResult validate(UUID surveyId, List<SubmitAnswerRequest> answers,
          UUID targetPageId, boolean throwOnError, Set<UUID> openPageIds) {
    List<Question> questions = openQuestions(surveyId, openPageIds);
    Map<UUID, Question> questionMap = new HashMap<>();

    for (Question question : questions) {
      questionMap.put(question.getId(), question);
    }

    Map<UUID, SubmitAnswerRequest> answerMap =
            validateSubmittedAnswerShape(surveyId, answers, questionMap);

    List<Condition> conditions =
            conditionRepository
                    .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                            surveyId,
                            surveyId
                    );

    Map<UUID, Boolean> applicability =
            ConditionUtil.calculateApplicability(
                    questions,
                    toConditionAnswers(answerMap),
                    conditions
            );

    Map<UUID, List<QuestionValidationRule>> rulesByQuestion = rulesByQuestion(surveyId);
    List<AnswerValidationError> errors = new ArrayList<>();
    List<SubmitAnswerRequest> visibleAnswers = new ArrayList<>();

    for (Question question : questions) {
      boolean visible = applicability.getOrDefault(question.getId(), true);
      SubmitAnswerRequest answer = answerMap.get(question.getId());

      if (targetPageId != null && !Objects.equals(question.getPage().getId(), targetPageId)) {
        continue;
      }

      if (!visible) {
        continue;
      }

      if (Boolean.TRUE.equals(question.getIsRequired()) && !hasValue(question, answer)) {
        errors.add(
                new AnswerValidationError(
                        question.getId(),
                        "REQUIRED",
                        answerField(question),
                        "Cau hoi bat buoc chua duoc tra loi"
                )
        );
        continue;
      }

      if (answer == null || !hasValue(question, answer)) {
        continue;
      }

      visibleAnswers.add(answer);
      validateRules(
              question,
              answer,
              rulesByQuestion.getOrDefault(question.getId(), List.of()),
              errors
      );
    }

    if (!errors.isEmpty() && throwOnError) {
      throw new AnswerValidationException(errors);
    }

    return new ValidationResult(errors.isEmpty(), visibleAnswers, errors, applicability);
  }

  private Map<UUID, SubmitAnswerRequest> validateSubmittedAnswerShape(
          UUID surveyId,
          List<SubmitAnswerRequest> answers,
          Map<UUID, Question> questionMap
  ) {
    Map<UUID, SubmitAnswerRequest> answerMap = new HashMap<>();

    if (answers == null) {
      return answerMap;
    }

    for (SubmitAnswerRequest answer : answers) {
      if (answer == null) {
        throw new BadRequestException("Cau tra loi khong hop le");
      }

      if (answer.getQuestionId() == null) {
        throw new BadRequestException("ID cau hoi khong duoc de trong");
      }

      if (answerMap.put(answer.getQuestionId(), answer) != null) {
        throw new BadRequestException("Cau hoi bi tra loi trung lap: " + answer.getQuestionId());
      }

      Question question = questionMap.get(answer.getQuestionId());

      if (question == null) {
        throw new BadRequestException("Question khong nam trong trang dang mo: " + answer.getQuestionId());
      }

      if (!Objects.equals(question.getPage().getSurvey().getId(), surveyId)) {
        throw new BadRequestException("Question khong thuoc survey nay");
      }

      validateAnswerType(question, answer);
      validateOptionOwnership(question, answer);
    }

    return answerMap;
  }

  private List<Question> openQuestions(UUID surveyId, Set<UUID> openPageIds) {
    List<Question> all = questionRepository.findByPage_Survey_Id(surveyId);
    if (openPageIds == null) return all;
    List<Condition> conditions = conditionRepository
        .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(surveyId, surveyId);
    Set<UUID> excluded = ConditionUtil.unreachableShowTargets(conditions, openPageIds);
    return all.stream().filter(q -> openPageIds.contains(q.getPage().getId())
        && !excluded.contains(q.getId())).toList();
  }

  private void validateRules(
          Question question,
          SubmitAnswerRequest answer,
          List<QuestionValidationRule> rules,
          List<AnswerValidationError> errors
  ) {
    Map<String, List<QuestionValidationRuleValue>> valuesByRule =
            ruleService.valuesByRule(question.getId());

    for (QuestionValidationRule rule : rules) {
      if (!Boolean.TRUE.equals(rule.getIsActive())) {
        continue;
      }

      String code = rule.getRuleCode();
      RuleDefinition definition = ruleService.definition(code);

      if (!matchesRule(
              answer,
              code,
              valuesByRule.getOrDefault(code, List.of())
      )) {
        errors.add(
                new AnswerValidationError(
                        question.getId(),
                        code,
                        answerField(question),
                        chooseMessage(rule, definition)
                )
        );
      }
    }
  }

  private boolean matchesRule(
          SubmitAnswerRequest answer,
          String code,
          List<QuestionValidationRuleValue> values
  ) {
    return switch (code) {
      case "MIN" -> answer.getAnswerNumber().compareTo(firstNumber(values, "value")) >= 0;
      case "MAX" -> answer.getAnswerNumber().compareTo(firstNumber(values, "value")) <= 0;
      case "GREATER_THAN" -> answer.getAnswerNumber().compareTo(firstNumber(values, "value")) > 0;
      case "LESS_THAN" -> answer.getAnswerNumber().compareTo(firstNumber(values, "value")) < 0;
      case "STEP" -> validStep(answer.getAnswerNumber(), values);
      case "ALLOWED_VALUES" -> numberSet(values, "value").contains(normalizeNumber(answer.getAnswerNumber()));
      case "DISALLOWED_VALUES" -> !numberSet(values, "value").contains(normalizeNumber(answer.getAnswerNumber()));
      case "MIN_LENGTH" -> codePointLength(answer.getAnswerText()) >= firstNumber(values, "value").intValue();
      case "MAX_LENGTH" -> codePointLength(answer.getAnswerText()) <= firstNumber(values, "value").intValue();
      case "REGEX" -> regexMatches(answer.getAnswerText(), firstText(values, "value"));
      case "EMAIL" -> EMAIL_PATTERN.matcher(nfc(answer.getAnswerText())).matches();
      case "PHONE" -> phoneMatches(answer.getAnswerText(), firstText(values, "country"));
      case "URL" -> validUrl(answer.getAnswerText());
      case "CHARACTER_SET" -> characterSet(answer.getAnswerText(), firstText(values, "value"));
      case "DIACRITICS" -> diacritics(answer.getAnswerText(), firstText(values, "value"));
      case "WHITESPACE" -> whitespace(answer.getAnswerText(), firstText(values, "value"));
      case "STARTS_WITH" -> nfc(answer.getAnswerText()).startsWith(nfc(firstText(values, "value")));
      case "ENDS_WITH" -> nfc(answer.getAnswerText()).endsWith(nfc(firstText(values, "value")));
      case "MUST_CONTAIN" -> containsAll(answer.getAnswerText(), textList(values, "value"));
      case "NOT_CONTAIN" -> containsNone(answer.getAnswerText(), textList(values, "value"));
      case "FIXED_OPTION" -> selectedOptions(answer).size() == 1
              && selectedOptions(answer).contains(firstOption(values, "value"));
      case "ALLOWED_OPTIONS" -> optionSet(values, "value").containsAll(selectedOptions(answer));
      case "DISALLOWED_OPTIONS" -> Collections.disjoint(optionSet(values, "value"), selectedOptions(answer));
      case "MIN_SELECTIONS" -> selectedOptions(answer).size() >= firstNumber(values, "value").intValue();
      case "MAX_SELECTIONS" -> selectedOptions(answer).size() <= firstNumber(values, "value").intValue();
      case "REQUIRED_OPTIONS" -> selectedOptions(answer).containsAll(optionSet(values, "value"));
      case "AT_MOST_ONE_OF" -> atMostOneOf(answer, values);
      case "EXCLUSIVE_OPTIONS" -> exclusiveOptions(answer, values);
      case "MIN_DATE" -> answerDate(answer).compareTo(firstDate(values, "value")) >= 0;
      case "MAX_DATE" -> answerDate(answer).compareTo(firstDate(values, "value")) <= 0;
      case "BEFORE_TODAY" -> answerDate(answer).isBefore(today());
      case "AFTER_TODAY" -> answerDate(answer).isAfter(today());
      case "NOT_FUTURE" -> !answerDate(answer).isAfter(today());
      case "NOT_PAST" -> !answerDate(answer).isBefore(today());
      case "MIN_AGE" -> Period.between(answerDate(answer), today()).getYears() >= firstNumber(values, "value").intValue();
      case "MAX_AGE" -> Period.between(answerDate(answer), today()).getYears() <= firstNumber(values, "value").intValue();
      case "ALLOWED_WEEKDAYS" -> numberSet(values, "value").contains(BigDecimal.valueOf(answerDate(answer).getDayOfWeek().getValue()));
      case "ALLOWED_DATES" -> dateSet(values, "value").contains(answerDate(answer));
      case "DISALLOWED_DATES" -> !dateSet(values, "value").contains(answerDate(answer));
      case "REQUIRE_PROVINCE" -> !isBlank(answer.getProvince());
      case "REQUIRE_WARD" -> !isBlank(answer.getWard());
      case "REQUIRE_DETAIL" -> !isBlank(answer.getAddressDetail());
      case "FIXED_PROVINCE" -> addressEquals(answer.getProvince(), firstText(values, "value"));
      case "FIXED_WARD" -> fixedWard(answer, values);
      case "ALLOWED_PROVINCES" -> addressSet(values, "value").contains(normalizeAddress(answer.getProvince()));
      case "DISALLOWED_PROVINCES" -> !addressSet(values, "value").contains(normalizeAddress(answer.getProvince()));
      case "ALLOWED_WARDS" -> addressPairSet(values).contains(addressPair(answer.getProvince(), answer.getWard()));
      case "DISALLOWED_WARDS" -> !addressPairSet(values).contains(addressPair(answer.getProvince(), answer.getWard()));
      case "DETAIL_MIN_LENGTH" -> codePointLength(answer.getAddressDetail()) >= firstNumber(values, "value").intValue();
      case "DETAIL_MAX_LENGTH" -> codePointLength(answer.getAddressDetail()) <= firstNumber(values, "value").intValue();
      case "DETAIL_REGEX" -> regexMatches(answer.getAddressDetail(), firstText(values, "value"));
      default -> true;
    };
  }

  private void validateAnswerType(Question question, SubmitAnswerRequest answer) {
    String type = getQuestionTypeCode(question);

    switch (type) {
      case "SINGLE_CHOICE" -> validateChoiceAnswer(question, answer, false);
      case "MULTIPLE_CHOICE" -> validateChoiceAnswer(question, answer, true);
      case "NUMBER" -> validateNumberAnswer(question, answer);
      case "DATE" -> validateDateAnswer(question, answer);
      case "ADDRESS" -> validateAddressAnswer(question, answer);
      default -> validateTextAnswer(question, answer);
    }
  }

  private void validateChoiceAnswer(Question question, SubmitAnswerRequest answer, boolean multiple) {
    if (hasText(answer)
            || answer.getAnswerNumber() != null
            || answer.getAnswerDate() != null
            || hasAddressValue(answer)
            || !isBlank(answer.getProvinceCode())
            || !isBlank(answer.getWardCode())) {
      throwInvalidAnswerType(question);
    }

    if (!multiple && selectedOptions(answer).size() > 1) {
      throw new BadRequestException("Cau hoi chi duoc chon mot dap an: " + question.getQuestionText());
    }

    Set<UUID> optionIds = new HashSet<>();

    for (UUID optionId : selectedOptions(answer)) {
      if (optionId == null || !optionIds.add(optionId)) {
        throw new BadRequestException("Option khong hop le");
      }
    }
  }

  private void validateNumberAnswer(Question question, SubmitAnswerRequest answer) {
    if (hasText(answer)
            || hasOptionIds(answer)
            || answer.getAnswerDate() != null
            || hasAddressValue(answer)
            || !isBlank(answer.getProvinceCode())
            || !isBlank(answer.getWardCode())) {
      throwInvalidAnswerType(question);
    }
  }

  private void validateDateAnswer(Question question, SubmitAnswerRequest answer) {
    if (hasText(answer)
            || hasOptionIds(answer)
            || answer.getAnswerNumber() != null
            || hasAddressValue(answer)
            || !isBlank(answer.getProvinceCode())
            || !isBlank(answer.getWardCode())) {
      throwInvalidAnswerType(question);
    }
  }

  private void validateAddressAnswer(Question question, SubmitAnswerRequest answer) {
    if (hasText(answer)
            || hasOptionIds(answer)
            || answer.getAnswerNumber() != null
            || answer.getAnswerDate() != null
            || !isBlank(answer.getProvinceCode())
            || !isBlank(answer.getWardCode())) {
      throwInvalidAnswerType(question);
    }
  }

  private void validateTextAnswer(Question question, SubmitAnswerRequest answer) {
    if (hasOptionIds(answer)
            || answer.getAnswerNumber() != null
            || answer.getAnswerDate() != null
            || hasAddressValue(answer)
            || !isBlank(answer.getProvinceCode())
            || !isBlank(answer.getWardCode())) {
      throwInvalidAnswerType(question);
    }
  }

  private void validateOptionOwnership(Question question, SubmitAnswerRequest answer) {
    if (!hasOptionIds(answer)) {
      return;
    }

    for (UUID optionId : answer.getOptionIds()) {
      if (optionId == null) {
        throw new BadRequestException("Option khong hop le");
      }

      if (!optionRepository.existsByIdAndQuestion_Id(optionId, question.getId())) {
        throw new BadRequestException("Option khong thuoc cau hoi nay");
      }
    }
  }

  private Map<UUID, List<QuestionValidationRule>> rulesByQuestion(UUID surveyId) {
    Map<UUID, List<QuestionValidationRule>> result = new HashMap<>();

    for (QuestionValidationRule rule : ruleService.activeRulesForSurvey(surveyId)) {
      if (rule.getQuestion() == null || rule.getQuestion().getId() == null) {
        continue;
      }

      result.computeIfAbsent(rule.getQuestion().getId(), ignored -> new ArrayList<>())
              .add(rule);
    }

    return result;
  }

  private List<Answer> toConditionAnswers(Map<UUID, SubmitAnswerRequest> requestAnswers) {
    List<Answer> answers = new ArrayList<>();

    for (SubmitAnswerRequest requestAnswer : requestAnswers.values()) {
      Question question = questionRepository.findById(requestAnswer.getQuestionId()).orElse(null);

      if (question == null) {
        continue;
      }

      Answer answer = new Answer();
      answer.setQuestion(question);
      answer.setAnswerText(requestAnswer.getAnswerText());
      answer.setAnswerNumber(requestAnswer.getAnswerNumber());
      answer.setAnswerDate(requestAnswer.getAnswerDate());
      answer.setProvince(requestAnswer.getProvince());
      answer.setWard(requestAnswer.getWard());
      answer.setAddressDetail(requestAnswer.getAddressDetail());

      List<AnswerOption> answerOptions = new ArrayList<>();

      for (UUID optionId : selectedOptions(requestAnswer)) {
        optionRepository.findById(optionId)
                .ifPresent(option -> {
                  AnswerOption answerOption = new AnswerOption();
                  answerOption.setAnswer(answer);
                  answerOption.setOption(option);
                  answerOptions.add(answerOption);
                });
      }

      answer.setAnswerOptions(answerOptions);
      answers.add(answer);
    }

    return answers;
  }

  private boolean validStep(BigDecimal actual, List<QuestionValidationRuleValue> values) {
    BigDecimal step = firstNumber(values, "step");
    BigDecimal base =
            Optional.ofNullable(firstNumber(values, "base"))
                    .orElse(BigDecimal.ZERO);

    if (step == null || step.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }

    return actual.subtract(base).remainder(step).compareTo(BigDecimal.ZERO) == 0;
  }

  private boolean atMostOneOf(SubmitAnswerRequest answer, List<QuestionValidationRuleValue> values) {
    for (Set<UUID> group : groupedOptions(values).values()) {
      int count = 0;

      for (UUID selected : selectedOptions(answer)) {
        if (group.contains(selected)) {
          count++;
        }
      }

      if (count > 1) {
        return false;
      }
    }

    return true;
  }

  private boolean exclusiveOptions(SubmitAnswerRequest answer, List<QuestionValidationRuleValue> values) {
    Set<UUID> exclusive = optionSet(values, "value");

    for (UUID selected : selectedOptions(answer)) {
      if (exclusive.contains(selected)) {
        return selectedOptions(answer).size() == 1;
      }
    }

    return true;
  }

  private Map<Integer, Set<UUID>> groupedOptions(List<QuestionValidationRuleValue> values) {
    Map<Integer, Set<UUID>> result = new HashMap<>();

    for (QuestionValidationRuleValue value : values) {
      if (value.getOption() == null) {
        continue;
      }

      result.computeIfAbsent(value.getGroupIndex(), ignored -> new HashSet<>())
              .add(value.getOption().getId());
    }

    return result;
  }

  private BigDecimal firstNumber(List<QuestionValidationRuleValue> values, String name) {
    return values.stream()
            .filter(value -> name.equals(value.getParameterName()))
            .map(QuestionValidationRuleValue::getNumberValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  private String firstText(List<QuestionValidationRuleValue> values, String name) {
    return values.stream()
            .filter(value -> name.equals(value.getParameterName()))
            .map(QuestionValidationRuleValue::getTextValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("");
  }

  private LocalDate firstDate(List<QuestionValidationRuleValue> values, String name) {
    return values.stream()
            .filter(value -> name.equals(value.getParameterName()))
            .map(QuestionValidationRuleValue::getDateValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  private UUID firstOption(List<QuestionValidationRuleValue> values, String name) {
    return values.stream()
            .filter(value -> name.equals(value.getParameterName()))
            .map(QuestionValidationRuleValue::getOption)
            .filter(Objects::nonNull)
            .map(Option::getId)
            .findFirst()
            .orElse(null);
  }

  private Set<BigDecimal> numberSet(List<QuestionValidationRuleValue> values, String name) {
    Set<BigDecimal> result = new HashSet<>();

    for (QuestionValidationRuleValue value : values) {
      if (name.equals(value.getParameterName()) && value.getNumberValue() != null) {
        result.add(normalizeNumber(value.getNumberValue()));
      }
    }

    return result;
  }

  private Set<String> textList(List<QuestionValidationRuleValue> values, String name) {
    Set<String> result = new HashSet<>();

    for (QuestionValidationRuleValue value : values) {
      if (name.equals(value.getParameterName()) && value.getTextValue() != null) {
        result.add(nfc(value.getTextValue()));
      }
    }

    return result;
  }

  private Set<UUID> optionSet(List<QuestionValidationRuleValue> values, String name) {
    Set<UUID> result = new HashSet<>();

    for (QuestionValidationRuleValue value : values) {
      if (name.equals(value.getParameterName()) && value.getOption() != null) {
        result.add(value.getOption().getId());
      }
    }

    return result;
  }

  private Set<LocalDate> dateSet(List<QuestionValidationRuleValue> values, String name) {
    Set<LocalDate> result = new HashSet<>();

    for (QuestionValidationRuleValue value : values) {
      if (name.equals(value.getParameterName()) && value.getDateValue() != null) {
        result.add(value.getDateValue());
      }
    }

    return result;
  }

  private Set<String> addressSet(List<QuestionValidationRuleValue> values, String name) {
    Set<String> result = new HashSet<>();

    for (QuestionValidationRuleValue value : values) {
      if (name.equals(value.getParameterName()) && value.getTextValue() != null) {
        result.add(normalizeAddress(value.getTextValue()));
      }
    }

    return result;
  }

  private Set<String> addressPairSet(List<QuestionValidationRuleValue> values) {
    Map<Integer, String> provinces = new HashMap<>();
    Map<Integer, String> wards = new HashMap<>();

    for (QuestionValidationRuleValue value : values) {
      if ("province".equals(value.getParameterName())) {
        provinces.put(value.getGroupIndex(), value.getTextValue());
      }

      if ("ward".equals(value.getParameterName())) {
        wards.put(value.getGroupIndex(), value.getTextValue());
      }
    }

    Set<String> result = new HashSet<>();

    for (Map.Entry<Integer, String> entry : provinces.entrySet()) {
      String ward = wards.get(entry.getKey());

      if (ward != null) {
        result.add(addressPair(entry.getValue(), ward));
      }
    }

    return result;
  }

  private boolean fixedWard(SubmitAnswerRequest answer, List<QuestionValidationRuleValue> values) {
    return addressPair(answer.getProvince(), answer.getWard())
            .equals(addressPair(firstText(values, "province"), firstText(values, "ward")));
  }

  private String addressPair(String province, String ward) {
    return normalizeAddress(province) + "|" + normalizeAddress(ward);
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private LocalDate answerDate(SubmitAnswerRequest answer) {
    return answer.getAnswerDate().toLocalDate();
  }

  private BigDecimal normalizeNumber(BigDecimal value) {
    return value.stripTrailingZeros();
  }

  private boolean regexMatches(String actual, String regex) {
    try {
      return Pattern.compile(regex).matcher(nfc(actual)).matches();
    } catch (PatternSyntaxException e) {
      return false;
    }
  }

  private boolean phoneMatches(String actual, String country) {
    if ("VN".equalsIgnoreCase(country) || "VIETNAM".equalsIgnoreCase(country)) {
      return nfc(actual).matches("^(0[35789])[0-9]{8}$");
    }

    return !isBlank(actual);
  }

  private boolean validUrl(String actual) {
    try {
      URI uri = URI.create(nfc(actual));
      return ("http".equalsIgnoreCase(uri.getScheme())
              || "https".equalsIgnoreCase(uri.getScheme()))
              && uri.getHost() != null;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean characterSet(String actual, String mode) {
    String value = nfc(actual).replaceAll("\\s+", "");

    return switch (mode == null ? "" : mode.trim().toUpperCase()) {
      case "DIGITS" -> value.matches("[0-9]*");
      case "LETTERS", "VIETNAMESE_LETTERS" -> value.matches("[\\p{L}]*");
      case "ALPHANUMERIC" -> value.matches("[\\p{L}\\p{N}]*");
      default -> true;
    };
  }

  private boolean diacritics(String actual, String mode) {
    String value = nfc(actual);
    boolean hasVietnameseDiacritic =
            value.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ].*");

    return switch (mode == null ? "" : mode.trim().toUpperCase()) {
      case "FORBID" -> !hasVietnameseDiacritic;
      case "REQUIRE" -> hasVietnameseDiacritic;
      default -> true;
    };
  }

  private boolean whitespace(String actual, String mode) {
    String value = nfc(actual);

    return switch (mode == null ? "" : mode.trim().toUpperCase()) {
      case "FORBID" -> !value.matches(".*\\s.*");
      case "SPACE_ONLY" -> !value.matches(".*[\\t\\n\\r\\f].*");
      default -> true;
    };
  }

  private boolean containsAll(String actual, Set<String> expected) {
    String value = nfc(actual);

    for (String item : expected) {
      if (!value.contains(item)) {
        return false;
      }
    }

    return true;
  }

  private boolean containsNone(String actual, Set<String> blocked) {
    String value = nfc(actual);

    for (String item : blocked) {
      if (value.contains(item)) {
        return false;
      }
    }

    return true;
  }

  private boolean addressEquals(String left, String right) {
    return normalizeAddress(left).equals(normalizeAddress(right));
  }

  private String normalizeAddress(String value) {
    return nfc(value).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private String nfc(String value) {
    return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC);
  }

  private int codePointLength(String value) {
    String normalized = nfc(value);
    return normalized.codePointCount(0, normalized.length());
  }

  private boolean hasValue(Question question, SubmitAnswerRequest answer) {
    if (answer == null) {
      return false;
    }

    return switch (getQuestionTypeCode(question)) {
      case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> hasOptionIds(answer);
      case "NUMBER" -> answer.getAnswerNumber() != null;
      case "DATE" -> answer.getAnswerDate() != null;
      case "ADDRESS" -> hasAddressValue(answer);
      default -> hasText(answer);
    };
  }

  private String answerField(Question question) {
    return switch (getQuestionTypeCode(question)) {
      case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> "optionIds";
      case "NUMBER" -> "answerNumber";
      case "DATE" -> "answerDate";
      case "ADDRESS" -> "address";
      default -> "answerText";
    };
  }

  private List<UUID> selectedOptions(SubmitAnswerRequest answer) {
    return answer.getOptionIds() == null ? List.of() : answer.getOptionIds();
  }

  private void throwInvalidAnswerType(Question question) {
    throw new BadRequestException(
            "Kieu du lieu cau tra loi khong dung voi loai cau hoi: "
                    + question.getQuestionText()
    );
  }

  private boolean hasText(SubmitAnswerRequest answer) {
    return !isBlank(answer.getAnswerText());
  }

  private boolean hasOptionIds(SubmitAnswerRequest answer) {
    return answer.getOptionIds() != null && !answer.getOptionIds().isEmpty();
  }

  private boolean hasAddressValue(SubmitAnswerRequest answer) {
    return !isBlank(answer.getProvince())
            || !isBlank(answer.getWard())
            || !isBlank(answer.getAddressDetail());
  }

  private String getQuestionTypeCode(Question question) {
    return question == null
            || question.getQuestionType() == null
            || question.getQuestionType().getCode() == null
            ? ""
            : question.getQuestionType().getCode().trim().toUpperCase();
  }

  private String chooseMessage(QuestionValidationRule rule, RuleDefinition definition) {
    if (rule.getErrorMessage() != null && !rule.getErrorMessage().isBlank()) {
      return rule.getErrorMessage();
    }

    return definition.defaultMessage();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public record ValidationResult(
          boolean valid,
          List<SubmitAnswerRequest> visibleAnswers,
          List<AnswerValidationError> fieldErrors,
          Map<UUID, Boolean> applicability
  ) {}
}
