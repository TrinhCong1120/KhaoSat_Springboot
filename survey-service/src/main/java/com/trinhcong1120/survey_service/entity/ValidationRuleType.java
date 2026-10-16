package com.trinhcong1120.survey_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "validation_rule_types")
public class ValidationRuleType {

  @Id
  @Column(name = "code", length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  public ValidationRuleType() {}

  public ValidationRuleType(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.isActive = true;
  }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
}
