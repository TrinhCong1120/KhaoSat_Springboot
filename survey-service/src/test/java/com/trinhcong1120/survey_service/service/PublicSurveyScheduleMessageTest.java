package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.SurveyApiException;
import com.trinhcong1120.survey_service.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

class PublicSurveyScheduleMessageTest {
  private final Instant now = Instant.parse("2026-10-08T05:00:00Z");

  @Test
  void publicMessageDistinguishesInitialScheduleFromGapAndClosure() {
    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    survey.setPublishedAt(now.minusSeconds(14400));
    Page first = new Page();
    first.setOpenAt(now.minusSeconds(7200));
    first.setCloseAt(now);
    Page next = new Page();
    next.setOpenAt(now.plusSeconds(3600));
    next.setCloseAt(now.plusSeconds(7200));
    List<Page> surveyPages = List.of(first, next);

    SurveyApiException beforeFirst = errorAt(survey, surveyPages, now.minusSeconds(10800));
    assertEquals(HttpStatus.CONFLICT, beforeFirst.getStatus());
    assertEquals("SURVEY_NOT_OPEN", beforeFirst.getCode());
    assertEquals("Khao sat hien chua duoc mo", beforeFirst.getMessage());

    SurveyApiException betweenPages = errorAt(survey, surveyPages, now);
    assertEquals(HttpStatus.CONFLICT, betweenPages.getStatus());
    assertEquals("SURVEY_NOT_OPEN", betweenPages.getCode());
    assertEquals("Khảo sát đã kết thúc một giai đoạn và đang chờ giai đoạn tiếp theo",
        betweenPages.getMessage());

    SurveyApiException afterLast = errorAt(survey, surveyPages, now.plusSeconds(7200));
    assertEquals(HttpStatus.GONE, afterLast.getStatus());
    assertEquals("SURVEY_CLOSED", afterLast.getCode());
  }

  private SurveyApiException errorAt(Survey survey, List<Page> surveyPages, Instant at) {
    SurveyRepository surveys = mock(SurveyRepository.class);
    PageRepository pages = mock(PageRepository.class);
    when(surveys.findForRead(survey.getId())).thenReturn(Optional.of(survey));
    when(pages.findBySurvey_IdOrderByOrderIndexAsc(survey.getId())).thenReturn(surveyPages);
    SurveyScheduleService schedule = new SurveyScheduleService(pages, Clock.fixed(at, ZoneOffset.UTC));
    PublicSurveyService service = new PublicSurveyService(
        surveys, pages, mock(QuestionRepository.class), mock(OptionRepository.class),
        mock(ConditionRepository.class), mock(ResponseRepository.class),
        mock(AnswerRepository.class), mock(AnswerOptionRepository.class), mock(MediaFileRepository.class),
        mock(AnswerValidationService.class), mock(QuestionValidationRuleService.class),
        mock(ObjectMapper.class), mock(TransactionTemplate.class), schedule);
    return assertThrows(SurveyApiException.class, () -> service.getActiveSurvey(survey.getId()));
  }
}
