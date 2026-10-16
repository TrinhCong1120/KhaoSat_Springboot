package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseDetailResponse;
import com.trinhcong1120.survey_service.dto.response.ResponsePageResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseQuestionResponse;
import com.trinhcong1120.survey_service.dto.survey.SurveyDetailResponse;
import com.trinhcong1120.survey_service.entity.MediaFile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MediaContractTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void surveyAndResponseExposeOnlyMediaFiles() throws Exception {
    MediaFile file = new MediaFile();
    file.setId(UUID.randomUUID());
    file.setOwnerType("QUESTION");
    file.setOwnerId(UUID.randomUUID());
    file.setMediaType("IMAGE");
    file.setObjectUrl("/api/survey-media/example.png");
    MediaUploadResponse media = MediaUploadResponse.fromEntity(file);

    SurveyDetailResponse survey = new SurveyDetailResponse();
    survey.setMediaFiles(List.of(media));
    String surveyJson = mapper.writeValueAsString(survey);
    assertTrue(surveyJson.contains("\"mediaFiles\""));
    assertTrue(surveyJson.contains("\"objectUrl\":\"/api/survey-media/example.png\""));
    assertFalse(surveyJson.contains("imageUrl"));
    assertFalse(surveyJson.contains("videoUrl"));
    assertFalse(surveyJson.contains("audioUrl"));

    ResponseQuestionResponse question = new ResponseQuestionResponse();
    question.setMediaFiles(List.of(media));
    ResponsePageResponse page = new ResponsePageResponse();
    page.setMediaFiles(List.of(media));
    page.setQuestions(List.of(question));
    ResponseDetailResponse response = new ResponseDetailResponse();
    response.setMediaFiles(List.of(media));
    response.setPages(List.of(page));
    String responseJson = mapper.writeValueAsString(response);
    assertTrue(responseJson.contains("\"mediaFiles\""));
    assertFalse(responseJson.contains("ImageUrl"));
    assertFalse(responseJson.contains("imageUrl"));
  }
}
