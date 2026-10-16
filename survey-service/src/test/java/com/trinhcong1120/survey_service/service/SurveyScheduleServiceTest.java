package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.PageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SurveyScheduleServiceTest {
  private final Instant now = Instant.parse("2026-10-08T05:00:00Z");
  private final PageRepository pages = mock(PageRepository.class);
  private final SurveyScheduleService schedule =
      new SurveyScheduleService(pages, Clock.fixed(now, ZoneOffset.UTC));

  private Survey publishedSurvey() {
    Survey survey = new Survey();
    survey.setId(UUID.randomUUID());
    survey.setPublishedAt(now.minusSeconds(3600));
    return survey;
  }

  @Test
  void halfOpenPageWindowAndGapBetweenPages() {
    Survey survey = publishedSurvey();
    Page first = new Page();
    first.setOpenAt(now.minusSeconds(7200));
    first.setCloseAt(now);
    Page next = new Page();
    next.setOpenAt(now.plusSeconds(3600));
    next.setCloseAt(now.plusSeconds(7200));
    assertFalse(schedule.available(survey, first, now));
    assertEquals(SurveyScheduleService.Status.SCHEDULED,
        schedule.status(survey, List.of(first, next), now));
    assertEquals(SurveyScheduleService.Status.OPEN,
        schedule.status(survey, List.of(first, next), now.plusSeconds(3600)));
    assertEquals(SurveyScheduleService.Status.CLOSED,
        schedule.status(survey, List.of(first, next), now.plusSeconds(7200)));
  }

  @Test
  void expiredSurveyNeedsExplicitUnlockAfterScheduleEdit() {
    Survey survey = publishedSurvey();
    survey.setCloseAt(now);
    Page page = new Page();
    when(pages.findBySurvey_IdOrderByOrderIndexAsc(survey.getId())).thenReturn(List.of(page));
    schedule.freezeIfExpired(survey, now);
    assertTrue(survey.isRequiresUnlock());
    survey.setCloseAt(now.plusSeconds(3600));
    assertEquals(SurveyScheduleService.Status.CLOSED, schedule.status(survey));
    survey.setRequiresUnlock(false);
    assertEquals(SurveyScheduleService.Status.OPEN, schedule.status(survey));
  }

  @Test
  void localTimeUsesVietnamZoneAndNonOverlappingPageRejected() {
    assertEquals(Instant.parse("2026-10-08T05:00:00Z"),
        schedule.toInstant(LocalDateTime.parse("2026-10-08T12:00:00")));
    Survey survey = publishedSurvey();
    survey.setOpenAt(now);
    survey.setCloseAt(now.plusSeconds(3600));
    assertThrows(BadRequestException.class,
        () -> schedule.validatePage(survey, now.plusSeconds(3600), now.plusSeconds(7200)));
  }
}
