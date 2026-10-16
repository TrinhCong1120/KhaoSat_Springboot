package com.trinhcong1120.survey_service.dto.validation;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RuleParameterResponse {

  private String name;
  private Integer groupIndex;
  private Integer position;
  private String textValue;
  private BigDecimal numberValue;
  private LocalDate dateValue;
  private UUID optionId;

  public RuleParameterResponse() {}

  public RuleParameterResponse(
          String name,
          Integer groupIndex,
          Integer position,
          String textValue,
          BigDecimal numberValue,
          LocalDate dateValue,
          UUID optionId
  ) {
    this.name = name;
    this.groupIndex = groupIndex;
    this.position = position;
    this.textValue = textValue;
    this.numberValue = numberValue;
    this.dateValue = dateValue;
    this.optionId = optionId;
  }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public Integer getGroupIndex() { return groupIndex; }
  public void setGroupIndex(Integer groupIndex) { this.groupIndex = groupIndex; }

  public Integer getPosition() { return position; }
  public void setPosition(Integer position) { this.position = position; }

  public String getTextValue() { return textValue; }
  public void setTextValue(String textValue) { this.textValue = textValue; }

  public BigDecimal getNumberValue() { return numberValue; }
  public void setNumberValue(BigDecimal numberValue) { this.numberValue = numberValue; }

  public LocalDate getDateValue() { return dateValue; }
  public void setDateValue(LocalDate dateValue) { this.dateValue = dateValue; }

  public UUID getOptionId() { return optionId; }
  public void setOptionId(UUID optionId) { this.optionId = optionId; }
}
