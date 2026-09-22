package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.page.*;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PageService {

  private final PageRepository pageRepository;
  private final SurveyRepository surveyRepository;

  public PageService(
          PageRepository pageRepository,
          SurveyRepository surveyRepository
  ) {
    this.pageRepository = pageRepository;
    this.surveyRepository = surveyRepository;
  }

  @Transactional(readOnly = true)
  public List<PageResponse> getBySurvey(Integer surveyId) {
    return pageRepository
            .findBySurvey_IdOrderByOrderIndexAsc(surveyId)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public Page getEntity(Integer id) {
    return pageRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException("Page không tồn tại"));
  }

  public PageResponse create(CreatePageRequest request) {

    Survey survey = surveyRepository
            .findById(request.getSurveyId())
            .orElseThrow(() ->
                    new NotFoundException("Survey không tồn tại"));

    Page page = new Page();

    page.setSurvey(survey);
    page.setTitle(request.getTitle());
    page.setOrderIndex(request.getOrderIndex());

    return toResponse(pageRepository.save(page));
  }

  public PageResponse update(
          Integer id,
          UpdatePageRequest request
  ) {
    Page page = getEntity(id);

    page.setTitle(request.getTitle());
    page.setOrderIndex(request.getOrderIndex());

    return toResponse(pageRepository.save(page));
  }

  public void delete(Integer id) {
    Page page = getEntity(id);
    pageRepository.delete(page);
  }

  private PageResponse toResponse(Page page) {
    return new PageResponse(
            page.getId(),
            page.getSurvey().getId(),
            page.getTitle(),
            page.getOrderIndex()
    );
  }
}