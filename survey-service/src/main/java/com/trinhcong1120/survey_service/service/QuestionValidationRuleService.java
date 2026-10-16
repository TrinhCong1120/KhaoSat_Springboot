package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.validation.*;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.validation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestionValidationRuleService {

  private static final int MAX_REGEX_LENGTH = 500;

  private final QuestionRepository questionRepository;
  private final SurveyRepository surveyRepository;
  private final OptionRepository optionRepository;
  private final ValidationRuleTypeRepository validationRuleTypeRepository;
  private final ValidationRuleTypeQuestionTypeRepository validationRuleTypeQuestionTypeRepository;
  private final QuestionValidationRuleRepository ruleRepository;
  private final QuestionValidationRuleValueRepository valueRepository;
  private final ValidationRuleRegistry registry;

  public QuestionValidationRuleService(
          QuestionRepository questionRepository,
          SurveyRepository surveyRepository,
          OptionRepository optionRepository,
          ValidationRuleTypeRepository validationRuleTypeRepository,
          ValidationRuleTypeQuestionTypeRepository validationRuleTypeQuestionTypeRepository,
          QuestionValidationRuleRepository ruleRepository,
          QuestionValidationRuleValueRepository valueRepository,
          ValidationRuleRegistry registry
  ) {
    this.questionRepository = questionRepository;
    this.surveyRepository = surveyRepository;
    this.optionRepository = optionRepository;
    this.validationRuleTypeRepository = validationRuleTypeRepository;
    this.validationRuleTypeQuestionTypeRepository = validationRuleTypeQuestionTypeRepository;
    this.ruleRepository = ruleRepository;
    this.valueRepository = valueRepository;
    this.registry = registry;
  }

  @Transactional(readOnly = true)
  public ValidationRuleCatalogResponse getCatalog(String questionType) {
    ValidationRuleCatalogResponse response = new ValidationRuleCatalogResponse();
    String normalizedQuestionType = normalizeQuestionType(questionType);
    List<String> ruleCodes =
            normalizedQuestionType.isBlank()
                    ? validationRuleTypeQuestionTypeRepository.findActiveRuleCodes()
                    : validationRuleTypeQuestionTypeRepository
                            .findActiveRuleCodesByQuestionTypeCode(normalizedQuestionType);
    Set<String> allowedRuleCodes = new HashSet<>(ruleCodes);

    response.setRules(
            registry.all()
                    .stream()
                    .filter(definition -> allowedRuleCodes.contains(definition.code()))
                    .map(this::toCatalogRule)
                    .toList()
    );

    return response;
  }

  @Transactional(readOnly = true)
  public List<QuestionValidationRuleResponse> getByQuestion(UUID questionId) {
    Question question = getQuestion(questionId);
    Map<String, List<QuestionValidationRuleValue>> valuesByRule =
            valuesByRule(questionId);

    return ruleRepository
            .findByQuestion_Id(question.getId())
            .stream()
            .sorted(ruleComparator())
            .map(rule -> toResponse(
                    rule,
                    valuesByRule.getOrDefault(rule.getRuleCode(), List.of())
            ))
            .toList();
  }

  public List<QuestionValidationRuleResponse> replaceRules(
          UUID questionId,
          UpdateQuestionValidationRulesRequest request
  ) {
    Question question = getQuestion(questionId);

    List<QuestionValidationRuleRequest> rules =
            request == null || request.getRules() == null
                    ? List.of()
                    : request.getRules();

    validateDefinition(question, rules);

    ruleRepository.deleteByQuestion_Id(questionId);
    ruleRepository.flush();

    for (QuestionValidationRuleRequest item : rules) {
      saveRule(question, item);
    }

    bumpRevision(question);

    return getByQuestion(questionId);
  }

  public void validateDraft(
          UUID questionId,
          UpdateQuestionValidationRulesRequest request
  ) {
    Question question = getQuestion(questionId);

    validateDefinition(
            question,
            request == null || request.getRules() == null
                    ? List.of()
                    : request.getRules()
    );
  }

  public void seedRuleTypes() {
    for (RuleDefinition definition : registry.all()) {
      ValidationRuleType type =
              validationRuleTypeRepository.findById(definition.code())
                      .orElseGet(ValidationRuleType::new);

      type.setCode(definition.code());
      type.setName(definition.name());
      type.setDescription(definition.description());

      if (type.getIsActive() == null) {
        type.setIsActive(true);
      }

      validationRuleTypeRepository.save(type);

      for (String questionType : definition.applicableQuestionTypes()) {
        ValidationRuleTypeQuestionTypeId id =
                new ValidationRuleTypeQuestionTypeId(definition.code(), questionType);

        if (!validationRuleTypeQuestionTypeRepository.existsById(id)) {
          validationRuleTypeQuestionTypeRepository.save(
                  new ValidationRuleTypeQuestionType(type, questionType)
          );
        }
      }
    }
  }

  public RuleDefinition definition(String code) {
    try {
      return registry.getRequired(code);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Rule khong duoc ho tro: " + code);
    }
  }

  public List<QuestionValidationRule> activeRulesForSurvey(UUID surveyId) {
    return ruleRepository
            .findByQuestion_Page_Survey_IdAndIsActiveTrue(surveyId)
            .stream()
            .sorted(
                    Comparator
                            .comparing(
                                    (QuestionValidationRule rule) ->
                                            rule.getQuestion() == null ? null : rule.getQuestion().getId(),
                                    Comparator.nullsLast(UUID::compareTo)
                            )
                            .thenComparing(ruleComparator())
            )
            .toList();
  }

  public Map<String, List<QuestionValidationRuleValue>> valuesByRule(UUID questionId) {
    Map<String, List<QuestionValidationRuleValue>> result = new HashMap<>();

    for (QuestionValidationRuleValue value :
            valueRepository
                    .findById_QuestionId(questionId)
                    .stream()
                    .sorted(ruleValueComparator())
                    .toList()) {

      result.computeIfAbsent(value.getId().getRuleCode(), ignored -> new ArrayList<>())
              .add(value);
    }

    return result;
  }

  private void validateDefinition(
          Question question,
          List<QuestionValidationRuleRequest> rules
  ) {
    Set<String> seenCodes = new HashSet<>();
    Map<String, QuestionValidationRuleRequest> byCode = new LinkedHashMap<>();

    for (QuestionValidationRuleRequest request : rules) {
      if (request == null) {
        throw new BadRequestException("Rule khong hop le");
      }

      String code = registry.normalize(request.getCode());

      if (code.isBlank()) {
        throw new BadRequestException("Rule code khong duoc de trong");
      }

      if (!seenCodes.add(code)) {
        throw new BadRequestException("Moi rule code chi duoc dung mot lan tren mot cau hoi");
      }

      RuleDefinition definition = getSupportedDefinition(code);
      String questionType = getQuestionTypeCode(question);

      if (!ruleAppliesToQuestionType(code, questionType)) {
        throw new BadRequestException(
                "Rule " + code + " khong phu hop voi loai cau hoi " + questionType
        );
      }

      validateParameters(question, definition, request);
      byCode.put(code, request);
    }

    validateConflicts(question, byCode);
  }

  private void validateParameters(
          Question question,
          RuleDefinition definition,
          QuestionValidationRuleRequest request
  ) {
    List<RuleParameterRequest> parameters =
            request.getParameters() == null
                    ? List.of()
                    : request.getParameters();

    Map<String, List<RuleParameterRequest>> byName = new HashMap<>();

    for (RuleParameterRequest parameter : parameters) {
      String name = normalizeParameterName(parameter.getName());
      byName.computeIfAbsent(name, ignored -> new ArrayList<>()).add(parameter);
      validateValueShape(question, definition, parameter, name);
    }

    for (ParameterSpec spec : definition.parameters()) {
      List<RuleParameterRequest> values =
              byName.getOrDefault(spec.name(), List.of());

      if (spec.required() && values.isEmpty()) {
        throw new BadRequestException(definition.code() + " thieu tham so " + spec.name());
      }

      if (!spec.multiple() && values.size() > 1) {
        throw new BadRequestException(definition.code() + " chi duoc co mot tham so " + spec.name());
      }
    }

    Set<String> allowedNames = new HashSet<>(
            definition.parameters()
                    .stream()
                    .map(ParameterSpec::name)
                    .toList()
    );

    for (String name : byName.keySet()) {
      if (!allowedNames.contains(name)) {
        throw new BadRequestException(definition.code() + " co tham so khong ho tro: " + name);
      }
    }

    if (isRegexRule(definition.code())) {
      for (RuleParameterRequest parameter : byName.getOrDefault("value", List.of())) {
        validateRegex(definition.code(), parameter.getTextValue());
      }
    }
  }

  private void validateValueShape(
          Question question,
          RuleDefinition definition,
          RuleParameterRequest parameter,
          String name
  ) {
    ParameterSpec spec =
            definition.parameters()
                    .stream()
                    .filter(item -> item.name().equals(name))
                    .findFirst()
                    .orElse(null);

    if (spec == null) {
      return;
    }

    int valueCount = 0;
    valueCount += parameter.getTextValue() == null ? 0 : 1;
    valueCount += parameter.getNumberValue() == null ? 0 : 1;
    valueCount += parameter.getDateValue() == null ? 0 : 1;
    valueCount += parameter.getOptionId() == null ? 0 : 1;

    if (valueCount != 1) {
      throw new BadRequestException(definition.code() + "." + name + " phai co dung mot kieu gia tri");
    }

    if (spec.type() == ParameterType.TEXT && parameter.getTextValue() == null) {
      throw new BadRequestException(definition.code() + "." + name + " phai la text");
    }

    if (spec.type() == ParameterType.NUMBER && parameter.getNumberValue() == null) {
      throw new BadRequestException(definition.code() + "." + name + " phai la number");
    }

    if (spec.type() == ParameterType.DATE && parameter.getDateValue() == null) {
      throw new BadRequestException(definition.code() + "." + name + " phai la date");
    }

    if (spec.type() == ParameterType.OPTION) {
      if (parameter.getOptionId() == null) {
        throw new BadRequestException(definition.code() + "." + name + " phai la optionId");
      }

      Option option =
              optionRepository.findById(parameter.getOptionId())
                      .orElseThrow(() -> new BadRequestException("Option khong ton tai"));

      if (!Objects.equals(option.getQuestion().getId(), question.getId())) {
        throw new BadRequestException("Option khong thuoc cau hoi");
      }
    }
  }

  private void validateConflicts(
          Question question,
          Map<String, QuestionValidationRuleRequest> rules
  ) {
    assertMinMax(rules, "MIN", "MAX");
    assertMinMax(rules, "MIN_LENGTH", "MAX_LENGTH");
    assertMinMax(rules, "MIN_SELECTIONS", "MAX_SELECTIONS");
    assertMinMax(rules, "MIN_DATE", "MAX_DATE");
    assertMinMax(rules, "DETAIL_MIN_LENGTH", "DETAIL_MAX_LENGTH");

    QuestionValidationRuleRequest step = rules.get("STEP");

    if (step != null) {
      BigDecimal stepValue = firstNumber(step, "step");

      if (stepValue == null || stepValue.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("STEP.step phai lon hon 0");
      }
    }

    QuestionValidationRuleRequest maxSelections = rules.get("MAX_SELECTIONS");

    if (maxSelections != null) {
      BigDecimal max = firstNumber(maxSelections, "value");
      int optionCount =
              optionRepository.findByQuestion_IdOrderByOrderIndexAsc(question.getId()).size();

      if (max != null && max.compareTo(BigDecimal.valueOf(optionCount)) > 0) {
        throw new BadRequestException("MAX_SELECTIONS khong duoc lon hon so option");
      }
    }

    Set<UUID> disallowed = optionIds(rules.get("DISALLOWED_OPTIONS"), "value");
    Set<UUID> required = optionIds(rules.get("REQUIRED_OPTIONS"), "value");

    for (UUID optionId : required) {
      if (disallowed.contains(optionId)) {
        throw new BadRequestException("Option vua REQUIRED_OPTIONS vua DISALLOWED_OPTIONS");
      }
    }

    UUID fixed = firstOption(rules.get("FIXED_OPTION"), "value");

    if (fixed != null) {
      Set<UUID> allowed = optionIds(rules.get("ALLOWED_OPTIONS"), "value");

      if (!allowed.isEmpty() && !allowed.contains(fixed)) {
        throw new BadRequestException("FIXED_OPTION nam ngoai ALLOWED_OPTIONS");
      }

      if (disallowed.contains(fixed)) {
        throw new BadRequestException("FIXED_OPTION nam trong DISALLOWED_OPTIONS");
      }
    }
  }

  private Comparator<QuestionValidationRule> ruleComparator() {
    return Comparator
            .comparing(
                    QuestionValidationRule::getOrderIndex,
                    Comparator.nullsLast(Integer::compareTo)
            )
            .thenComparing(
                    QuestionValidationRule::getRuleCode,
                    Comparator.nullsLast(String::compareTo)
            );
  }

  private Comparator<QuestionValidationRuleValue> ruleValueComparator() {
    return Comparator
            .comparing(
                    (QuestionValidationRuleValue value) ->
                            value.getId() == null ? null : value.getId().getRuleCode(),
                    Comparator.nullsLast(String::compareTo)
            )
            .thenComparing(
                    QuestionValidationRuleValue::getParameterName,
                    Comparator.nullsLast(String::compareTo)
            )
            .thenComparing(
                    QuestionValidationRuleValue::getGroupIndex,
                    Comparator.nullsLast(Integer::compareTo)
            )
            .thenComparing(
                    QuestionValidationRuleValue::getPosition,
                    Comparator.nullsLast(Integer::compareTo)
            );
  }

  private void assertMinMax(
          Map<String, QuestionValidationRuleRequest> rules,
          String minCode,
          String maxCode
  ) {
    QuestionValidationRuleRequest minRule = rules.get(minCode);
    QuestionValidationRuleRequest maxRule = rules.get(maxCode);

    if (minRule == null || maxRule == null) {
      return;
    }

    BigDecimal minNumber = firstNumber(minRule, "value");
    BigDecimal maxNumber = firstNumber(maxRule, "value");

    if (minNumber != null && maxNumber != null && minNumber.compareTo(maxNumber) > 0) {
      throw new BadRequestException(minCode + " khong duoc lon hon " + maxCode);
    }

    LocalDate minDate = firstDate(minRule, "value");
    LocalDate maxDate = firstDate(maxRule, "value");

    if (minDate != null && maxDate != null && minDate.isAfter(maxDate)) {
      throw new BadRequestException(minCode + " khong duoc lon hon " + maxCode);
    }
  }

  private void saveRule(Question question, QuestionValidationRuleRequest request) {
    String code = registry.normalize(request.getCode());

    ValidationRuleType type =
            validationRuleTypeRepository.findById(code)
                    .orElseGet(() -> {
                      RuleDefinition definition = getSupportedDefinition(code);
                      return new ValidationRuleType(
                              definition.code(),
                              definition.name(),
                              definition.description()
                      );
                    });

    QuestionValidationRule rule = new QuestionValidationRule();
    rule.setId(new QuestionValidationRuleId(question.getId(), code));
    rule.setQuestion(question);
    rule.setRuleType(type);
    rule.setErrorMessage(request.getErrorMessage());
    rule.setIsActive(!Boolean.FALSE.equals(request.getIsActive()));
    rule.setOrderIndex(request.getOrderIndex() == null ? 0 : request.getOrderIndex());

    rule = ruleRepository.save(rule);

    List<RuleParameterRequest> parameters =
            request.getParameters() == null ? List.of() : request.getParameters();

    List<QuestionValidationRuleValue> values = new ArrayList<>();

    for (RuleParameterRequest parameter : parameters) {
      QuestionValidationRuleValue value = new QuestionValidationRuleValue();
      value.setId(
              new QuestionValidationRuleValueId(
                      question.getId(),
                      code,
                      normalizeParameterName(parameter.getName()),
                      parameter.getGroupIndex() == null ? 0 : parameter.getGroupIndex(),
                      parameter.getPosition() == null ? 0 : parameter.getPosition()
              )
      );
      value.setRule(rule);
      value.setTextValue(parameter.getTextValue());
      value.setNumberValue(parameter.getNumberValue());
      value.setDateValue(parameter.getDateValue());

      if (parameter.getOptionId() != null) {
        value.setOption(optionRepository.findById(parameter.getOptionId()).orElseThrow());
      }

      values.add(value);
    }

    valueRepository.saveAll(values);
  }

  private Question getQuestion(UUID questionId) {
    return questionRepository.findById(questionId)
            .orElseThrow(() -> new NotFoundException("Question khong ton tai"));
  }

  private RuleDefinition getSupportedDefinition(String code) {
    try {
      return registry.getRequired(code);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Rule khong duoc ho tro: " + code);
    }
  }

  private void bumpRevision(Question question) {
    Survey survey = question.getPage().getSurvey();
    survey.incrementValidationRevision();
    surveyRepository.save(survey);
  }

  private QuestionValidationRuleResponse toResponse(
          QuestionValidationRule rule,
          List<QuestionValidationRuleValue> values
  ) {
    RuleDefinition definition = registry.get(rule.getRuleCode());
    QuestionValidationRuleResponse response = new QuestionValidationRuleResponse();

    response.setCode(rule.getRuleCode());
    response.setName(definition == null ? rule.getRuleCode() : definition.name());
    response.setDefaultMessage(definition == null ? null : definition.defaultMessage());
    response.setErrorMessage(rule.getErrorMessage());
    response.setIsActive(rule.getIsActive());
    response.setOrderIndex(rule.getOrderIndex());
    response.setParameters(
            values.stream()
                    .sorted(Comparator.comparing(QuestionValidationRuleValue::getParameterName)
                            .thenComparing(QuestionValidationRuleValue::getGroupIndex)
                            .thenComparing(QuestionValidationRuleValue::getPosition))
                    .map(value -> new RuleParameterResponse(
                            value.getParameterName(),
                            value.getGroupIndex(),
                            value.getPosition(),
                            value.getTextValue(),
                            value.getNumberValue(),
                            value.getDateValue(),
                            value.getOption() == null ? null : value.getOption().getId()
                    ))
                    .toList()
    );

    return response;
  }

  private ValidationRuleCatalogResponse.RuleDefinitionResponse toCatalogRule(
          RuleDefinition definition
  ) {
    ValidationRuleCatalogResponse.RuleDefinitionResponse response =
            new ValidationRuleCatalogResponse.RuleDefinitionResponse();

    response.setCode(definition.code());
    response.setName(definition.name());
    response.setDescription(definition.description());
    response.setApplicableQuestionTypes(
            validationRuleTypeQuestionTypeRepository
                    .findQuestionTypeCodesByRuleCode(definition.code())
    );
    response.setEditor(definition.editor());
    response.setDefaultMessage(definition.defaultMessage());
    response.setParameters(
            definition.parameters()
                    .stream()
                    .map(parameter ->
                            new ValidationRuleCatalogResponse.ParameterDefinitionResponse(
                                    parameter.name(),
                                    parameter.type().name(),
                                    parameter.required(),
                                    parameter.defaultValue()
                            ))
                    .toList()
    );

    return response;
  }

  private String getQuestionTypeCode(Question question) {
    return question.getQuestionType() == null
            || question.getQuestionType().getCode() == null
            ? ""
            : question.getQuestionType().getCode().trim().toUpperCase();
  }

  private String normalizeQuestionType(String value) {
    return value == null || value.isBlank() ? "" : value.trim().toUpperCase();
  }

  private boolean ruleAppliesToQuestionType(String ruleCode, String questionTypeCode) {
    if (questionTypeCode == null || questionTypeCode.isBlank()) {
      return false;
    }

    return validationRuleTypeQuestionTypeRepository
            .existsActiveByRuleCodeAndQuestionTypeCode(ruleCode, questionTypeCode);
  }

  private String normalizeParameterName(String value) {
    return value == null || value.isBlank() ? "value" : value.trim();
  }

  private boolean isRegexRule(String code) {
    return "REGEX".equals(code) || "DETAIL_REGEX".equals(code);
  }

  private void validateRegex(String code, String regex) {
    if (regex == null || regex.isBlank()) {
      throw new BadRequestException(code + " regex khong duoc de trong");
    }

    if (regex.length() > MAX_REGEX_LENGTH) {
      throw new BadRequestException(code + " regex qua dai");
    }

    try {
      Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      throw new BadRequestException(code + " regex sai cu phap");
    }
  }

  private BigDecimal firstNumber(QuestionValidationRuleRequest rule, String name) {
    if (rule == null || rule.getParameters() == null) {
      return null;
    }

    return rule.getParameters()
            .stream()
            .filter(parameter -> normalizeParameterName(parameter.getName()).equals(name))
            .map(RuleParameterRequest::getNumberValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  private LocalDate firstDate(QuestionValidationRuleRequest rule, String name) {
    if (rule == null || rule.getParameters() == null) {
      return null;
    }

    return rule.getParameters()
            .stream()
            .filter(parameter -> normalizeParameterName(parameter.getName()).equals(name))
            .map(RuleParameterRequest::getDateValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  private UUID firstOption(QuestionValidationRuleRequest rule, String name) {
    if (rule == null || rule.getParameters() == null) {
      return null;
    }

    return rule.getParameters()
            .stream()
            .filter(parameter -> normalizeParameterName(parameter.getName()).equals(name))
            .map(RuleParameterRequest::getOptionId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  private Set<UUID> optionIds(QuestionValidationRuleRequest rule, String name) {
    Set<UUID> result = new HashSet<>();

    if (rule == null || rule.getParameters() == null) {
      return result;
    }

    for (RuleParameterRequest parameter : rule.getParameters()) {
      if (normalizeParameterName(parameter.getName()).equals(name)
              && parameter.getOptionId() != null) {
        result.add(parameter.getOptionId());
      }
    }

    return result;
  }
}
