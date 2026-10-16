package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trinhcong1120.survey_service.dto.submit.SubmitSurveyRequest;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.SurveyApiException;
import com.trinhcong1120.survey_service.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class PublicSurveyIdempotencyTest {
  @Test
  void successfulRetryReturnsOldResponseBeforeCheckingClosedSchedule() {
    UUID surveyId = UUID.randomUUID(), requestId = UUID.randomUUID(), responseId = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setId(surveyId);
    Response old = new Response();
    old.setId(responseId);
    old.setSurvey(survey);
    old.setRequestId(requestId);
    ResponseRepository responses = mock(ResponseRepository.class);
    when(responses.findByRequestId(requestId)).thenReturn(Optional.of(old));
    TransactionTemplate transaction = mock(TransactionTemplate.class);
    when(transaction.execute(any())).thenAnswer(invocation ->
        ((org.springframework.transaction.support.TransactionCallback<?>) invocation.getArgument(0))
            .doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
    PublicSurveyService service = new PublicSurveyService(
        mock(SurveyRepository.class), mock(PageRepository.class), mock(QuestionRepository.class),
        mock(OptionRepository.class), mock(ConditionRepository.class), responses,
        mock(AnswerRepository.class), mock(AnswerOptionRepository.class), mock(MediaFileRepository.class),
        mock(AnswerValidationService.class), mock(QuestionValidationRuleService.class),
        new ObjectMapper().registerModule(new JavaTimeModule()), transaction,
        mock(SurveyScheduleService.class));
    SubmitSurveyRequest request = new SubmitSurveyRequest();
    request.setRequestId(requestId);
    assertEquals(responseId, service.submit(surveyId, request).getResponseId());
    assertThrows(SurveyApiException.class, () -> service.submit(UUID.randomUUID(), request));
    old.setRequestHash("different-payload");
    assertThrows(SurveyApiException.class, () -> service.submit(surveyId, request));
  }
}
