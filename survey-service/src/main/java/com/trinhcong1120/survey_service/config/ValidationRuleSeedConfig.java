package com.trinhcong1120.survey_service.config;

import com.trinhcong1120.survey_service.service.QuestionValidationRuleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationRuleSeedConfig {

  @Bean
  CommandLineRunner seedValidationRuleTypes(QuestionValidationRuleService questionValidationRuleService) {
    return args -> questionValidationRuleService.seedRuleTypes();
  }
}
