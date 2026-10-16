package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.repository.AnswerOptionRepository;
import com.trinhcong1120.survey_service.repository.AnswerRepository;
import com.trinhcong1120.survey_service.repository.ConditionRepository;
import com.trinhcong1120.survey_service.repository.MediaFileRepository;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.QuestionRepository;
import com.trinhcong1120.survey_service.repository.ResponseRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ResponseServicePaginationTest {

  @Test
  void readsOnlyTheRequestedDatabasePage() {
    ResponseRepository responses = mock(ResponseRepository.class);
    SurveyAccessGuard guard = mock(SurveyAccessGuard.class);
    ResponseService service = new ResponseService(
            responses,
            guard,
            mock(AnswerRepository.class),
            mock(AnswerOptionRepository.class),
            mock(PageRepository.class),
            mock(QuestionRepository.class),
            mock(ConditionRepository.class),
            mock(MediaFileRepository.class)
    );
    UUID surveyId = UUID.randomUUID();
    when(responses.findAll(
            any(Specification.class),
            any(Pageable.class)
    )).thenAnswer(invocation -> {
      Pageable pageable = invocation.getArgument(1);
      assertEquals(2, pageable.getPageNumber());
      assertEquals(10, pageable.getPageSize());
      assertEquals("submittedAt: DESC,id: DESC", pageable.getSort().toString());
      return new PageImpl<Response>(List.of(), pageable, 25);
    });

    var result = service.getBySurvey(surveyId, 3, null);

    verify(guard).view(surveyId);
    assertEquals(3, result.page());
    assertEquals(10, result.size());
    assertEquals(25, result.totalElements());
    assertEquals(3, result.totalPages());
    assertTrue(result.last());
  }
}
