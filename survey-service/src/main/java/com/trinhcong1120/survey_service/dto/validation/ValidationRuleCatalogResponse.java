package com.trinhcong1120.survey_service.dto.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidationRuleCatalogResponse {

  private String catalogVersion = "1";
  private String execution = "SERVER";
  private Map<String, String> regex = Map.of(
          "dialect", "JAVA_PATTERN",
          "matchMode", "FULL"
  );
  private List<RuleDefinitionResponse> rules = new ArrayList<>();

  public String getCatalogVersion() { return catalogVersion; }
  public void setCatalogVersion(String catalogVersion) { this.catalogVersion = catalogVersion; }

  public String getExecution() { return execution; }
  public void setExecution(String execution) { this.execution = execution; }

  public Map<String, String> getRegex() { return regex; }
  public void setRegex(Map<String, String> regex) { this.regex = regex; }

  public List<RuleDefinitionResponse> getRules() { return rules; }
  public void setRules(List<RuleDefinitionResponse> rules) { this.rules = rules; }

  public static class RuleDefinitionResponse {
    private String code;
    private String name;
    private String description;
    private List<String> applicableQuestionTypes;
    private List<ParameterDefinitionResponse> parameters;
    private String editor;
    private String defaultMessage;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getApplicableQuestionTypes() { return applicableQuestionTypes; }
    public void setApplicableQuestionTypes(List<String> applicableQuestionTypes) {
      this.applicableQuestionTypes = applicableQuestionTypes;
    }

    public List<ParameterDefinitionResponse> getParameters() { return parameters; }
    public void setParameters(List<ParameterDefinitionResponse> parameters) {
      this.parameters = parameters;
    }

    public String getEditor() { return editor; }
    public void setEditor(String editor) { this.editor = editor; }

    public String getDefaultMessage() { return defaultMessage; }
    public void setDefaultMessage(String defaultMessage) { this.defaultMessage = defaultMessage; }
  }

  public static class ParameterDefinitionResponse {
    private String name;
    private String type;
    private Boolean required;
    private String defaultValue;

    public ParameterDefinitionResponse() {}

    public ParameterDefinitionResponse(
            String name,
            String type,
            Boolean required,
            String defaultValue
    ) {
      this.name = name;
      this.type = type;
      this.required = required;
      this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
  }
}
