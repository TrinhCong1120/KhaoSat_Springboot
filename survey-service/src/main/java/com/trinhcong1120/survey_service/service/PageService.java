package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.page.*;
import com.trinhcong1120.survey_service.entity.MediaFile;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.MediaFileRepository;
import com.trinhcong1120.survey_service.repository.PageRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PageService {

  private final PageRepository pageRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final SurveyRepository surveyRepository;
  private final MediaFileRepository mediaFileRepository;

  public PageService(
          PageRepository pageRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          SurveyRepository surveyRepository,
          MediaFileRepository mediaFileRepository
  ) {
    this.pageRepository = pageRepository;
    this.guard = guard;
    this.surveyRepository = surveyRepository;
    this.mediaFileRepository = mediaFileRepository;
  }

  @Transactional(readOnly = true)
  public List<PageResponse> getBySurvey(UUID surveyId) {
    guard.view(surveyId);
    return pageRepository
            .findBySurvey_IdOrderByOrderIndexAsc(surveyId)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public Page getEntity(UUID id) {
    return pageRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException("Page không tồn tại"));
  }

  public PageResponse create(CreatePageRequest request) {
    guard.edit(request.getSurveyId());

    Survey survey = surveyRepository
            .findById(request.getSurveyId())
            .orElseThrow(() ->
                    new NotFoundException("Survey không tồn tại"));

    Page page = new Page();

    page.setSurvey(survey);
    page.setTitle(request.getTitle());
    page.setDescription(request.getDescription());
    page.setOrderIndex(request.getOrderIndex());

    return toResponse(pageRepository.save(page));
  }

  public PageResponse update(
          UUID id,
          UpdatePageRequest request
  ) {
    Page page = getEntity(id);
    guard.edit(page.getSurvey().getId());

    page.setTitle(request.getTitle());
    page.setDescription(request.getDescription());
    page.setOrderIndex(request.getOrderIndex());

    return toResponse(pageRepository.save(page));
  }

  public void delete(UUID id) {
    Page page = getEntity(id);
    guard.edit(page.getSurvey().getId());
    pageRepository.delete(page);
  }

  private PageResponse toResponse(Page page) {
    PageResponse response = new PageResponse(
            page.getId(),
            page.getSurvey().getId(),
            page.getTitle(),
            page.getDescription(),
            page.getOrderIndex()
    );

    response.setMediaFiles(toMediaResponses(page.getId()));
    return response;
  }

  private List<MediaUploadResponse> toMediaResponses(UUID pageId) {
    return mediaFileRepository
            .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc("PAGE", pageId)
            .stream()
            .map(MediaUploadResponse::fromEntity)
            .toList();
  }


}
