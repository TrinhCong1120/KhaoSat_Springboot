package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trinhcong1120.survey_service.dto.page.UpdatePageRequest;
import com.trinhcong1120.survey_service.dto.survey.UpdateSurveyRequest;
import org.junit.jupiter.api.Test;

class ScheduleRequestJsonTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void omittedTimeKeepsScheduleButExplicitNullClearsIt() throws Exception {
    UpdateSurveyRequest survey = mapper.readValue("{\"title\":\"Test\",\"openAt\":null}", UpdateSurveyRequest.class);
    assertTrue(survey.isOpenAtPresent());
    assertNull(survey.getOpenAt());
    assertFalse(survey.isCloseAtPresent());

    UpdatePageRequest page = mapper.readValue("{\"title\":\"Page\",\"closeAt\":null}", UpdatePageRequest.class);
    assertFalse(page.isOpenAtPresent());
    assertTrue(page.isCloseAtPresent());
    assertNull(page.getCloseAt());
  }
}
