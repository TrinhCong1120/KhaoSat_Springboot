package com.trinhcong1120.survey_service.validation;

public record ParameterSpec(
        String name,
        ParameterType type,
        boolean required,
        boolean multiple,
        String defaultValue
) {
  public static ParameterSpec required(String name, ParameterType type) {
    return new ParameterSpec(name, type, true, false, null);
  }

  public static ParameterSpec optional(String name, ParameterType type, String defaultValue) {
    return new ParameterSpec(name, type, false, false, defaultValue);
  }

  public static ParameterSpec list(String name, ParameterType type) {
    return new ParameterSpec(name, type, true, true, null);
  }
}
