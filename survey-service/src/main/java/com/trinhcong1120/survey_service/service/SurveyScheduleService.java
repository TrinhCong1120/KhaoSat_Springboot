package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.PageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SurveyScheduleService {
  public enum Status { DRAFT, SCHEDULED, OPEN, CLOSED }

  public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final Clock clock;
  private final PageRepository pages;

  @Autowired
  public SurveyScheduleService(PageRepository pages) {
    this(pages, Clock.systemUTC());
  }

  SurveyScheduleService(PageRepository pages, Clock clock) {
    this.pages = pages;
    this.clock = clock;
  }

  public Instant now() { return clock.instant(); }
  public Instant toInstant(LocalDateTime value) {
    return value == null ? null : value.atZone(ZONE).toInstant();
  }
  public LocalDateTime toLocal(Instant value) {
    return value == null ? null : LocalDateTime.ofInstant(value, ZONE);
  }

  public void validate(Instant open, Instant close) {
    if (open != null && close != null && !open.isBefore(close))
      throw new BadRequestException("Thoi gian mo phai truoc thoi gian dong");
  }

  public Instant effectiveOpen(Survey survey, Page page) {
    Instant a = survey.getOpenAt(), b = page.getOpenAt();
    if (a == null) return b;
    return b == null || a.isAfter(b) ? a : b;
  }

  public Instant effectiveClose(Survey survey, Page page) {
    Instant a = survey.getCloseAt(), b = page.getCloseAt();
    if (a == null) return b;
    return b == null || a.isBefore(b) ? a : b;
  }

  public void validatePage(Survey survey, Instant open, Instant close) {
    validate(open, close);
    Instant effectiveOpen = survey.getOpenAt() == null ? open
        : open == null || survey.getOpenAt().isAfter(open) ? survey.getOpenAt() : open;
    Instant effectiveClose = survey.getCloseAt() == null ? close
        : close == null || survey.getCloseAt().isBefore(close) ? survey.getCloseAt() : close;
    validate(effectiveOpen, effectiveClose);
  }

  public boolean available(Survey survey, Page page, Instant at) {
    Instant open = effectiveOpen(survey, page), close = effectiveClose(survey, page);
    return (open == null || !at.isBefore(open)) && (close == null || at.isBefore(close));
  }

  public boolean canOpenLater(Survey survey, Page page, Instant at) {
    Instant close = effectiveClose(survey, page);
    return close == null || at.isBefore(close);
  }

  public Status status(Survey survey) { return status(survey, pages.findBySurvey_IdOrderByOrderIndexAsc(survey.getId()), now()); }

  public Status status(Survey survey, List<Page> surveyPages, Instant at) {
    if (survey.getPublishedAt() == null || surveyPages.isEmpty()) return Status.DRAFT;
    if (survey.isManuallyLocked() || survey.isRequiresUnlock()
        || survey.getCloseAt() != null && !at.isBefore(survey.getCloseAt())) return Status.CLOSED;
    if (survey.getOpenAt() != null && at.isBefore(survey.getOpenAt())) return Status.SCHEDULED;
    if (surveyPages.stream().anyMatch(p -> available(survey, p, at))) return Status.OPEN;
    return surveyPages.stream().anyMatch(p -> canOpenLater(survey, p, at))
        ? Status.SCHEDULED : Status.CLOSED;
  }

  public List<Page> openPages(Survey survey, Instant at) {
    return pages.findBySurvey_IdOrderByOrderIndexAsc(survey.getId()).stream()
        .filter(p -> available(survey, p, at)).toList();
  }

  public boolean hasPossiblePage(Survey survey, List<Page> surveyPages, Instant at) {
    return survey.getCloseAt() == null || at.isBefore(survey.getCloseAt())
        ? surveyPages.stream().anyMatch(p -> canOpenLater(survey, p, at)) : false;
  }

  public void freezeIfExpired(Survey survey, Instant at) {
    if (survey.getPublishedAt() != null && !survey.isManuallyLocked()
        && status(survey, pages.findBySurvey_IdOrderByOrderIndexAsc(survey.getId()), at) == Status.CLOSED)
      survey.setRequiresUnlock(true);
  }
}
