package com.trinhcong1120.survey_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SurveyServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SurveyServiceApplication.class, args);
  }
}
