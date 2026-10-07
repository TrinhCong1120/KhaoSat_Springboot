package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.OptionResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;
import com.trinhcong1120.survey_service.dto.survey.*;
import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.MediaFile;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SurveyService {

  private final SurveyRepository surveyRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final SurveyAccessRepository accessRepository;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final ConditionRepository conditionRepository;
  private final ResponseRepository responseRepository;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final MediaFileRepository mediaFileRepository;
  private final QuestionValidationRuleService questionValidationRuleService;

  public SurveyService(
          SurveyRepository surveyRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          SurveyAccessRepository accessRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ConditionRepository conditionRepository,
          ResponseRepository responseRepository,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          MediaFileRepository mediaFileRepository,
          QuestionValidationRuleService questionValidationRuleService
  ) {
    this.surveyRepository = surveyRepository;
    this.guard = guard;
    this.accessRepository = accessRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.conditionRepository = conditionRepository;
    this.responseRepository = responseRepository;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.mediaFileRepository = mediaFileRepository;
    this.questionValidationRuleService = questionValidationRuleService;
  }

  @Transactional(readOnly = true)
  public List<SurveyResponse> getAll() {
    return guard.visibleSurveys()
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public Survey getEntity(UUID id) {
    return surveyRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException("Survey khong ton tai"));
  }

  @Transactional(readOnly = true)
  public SurveyDetailResponse getDetail(UUID id) {
    guard.view(id);
    Survey survey = getEntity(id);

    SurveyDetailResponse response = new SurveyDetailResponse();
    response.setId(survey.getId());
    response.setTitle(survey.getTitle());
    response.setDescription(survey.getDescription());
    response.setCreatorUser(survey.getCreatorUser());
    response.setCreatorUserId(survey.getCreatorUserId());
    response.setCreatedAt(survey.getCreatedAt());
    response.setIsActive(survey.getIsActive());
    response.setValidationRevision(survey.getValidationRevision());
    response.setMediaFiles(toMediaResponses("SURVEY", survey.getId()));
    response.setPages(toPageDetailResponses(id));
    response.setConditions(toConditionResponses(id));

    return response;
  }

  public SurveyResponse create(
          CreateSurveyRequest request,
          String username
  ) {
    Survey survey = new Survey();

    survey.setTitle(request.getTitle());
    survey.setDescription(request.getDescription());
    survey.setCreatorUser(username);
    survey.setCreatorUserId(guard.userId());
    survey.setCreatedAt(LocalDateTime.now());
    survey.setIsActive(false);

    return toResponse(surveyRepository.save(survey));
  }

  public SurveyResponse update(
          UUID id,
          UpdateSurveyRequest request
  ) {
    guard.edit(id);
    Survey survey = getEntity(id);

    survey.setTitle(request.getTitle());
    survey.setDescription(request.getDescription());

    return toResponse(surveyRepository.save(survey));
  }

  public SurveyResponse updateStatus(
          UUID id,
          UpdateSurveyStatusRequest request
  ) {
    guard.edit(id);
    Survey survey = getEntity(id);

    survey.setIsActive(request.getIsActive());

    return toResponse(surveyRepository.save(survey));
  }

  public void delete(UUID id) {
    guard.delete(id);
    Survey survey = getEntity(id);

    accessRepository.deleteBySurvey_Id(id);

    conditionRepository.deleteBySurveyId(id);
    answerOptionRepository.deleteBySurveyId(id);
    answerRepository.deleteBySurveyId(id);
    responseRepository.deleteBySurveyId(id);
    optionRepository.deleteBySurveyId(id);
    questionRepository.deleteBySurveyId(id);
    pageRepository.deleteBySurveyId(id);

    surveyRepository.delete(survey);
  }

  private List<SurveyDetailResponse.PageDetailResponse> toPageDetailResponses(
          UUID surveyId
  ) {
    return pageRepository.findBySurvey_IdOrderByOrderIndexAsc(surveyId)
            .stream()
            .map(this::toPageDetailResponse)
            .toList();
  }

  private SurveyDetailResponse.PageDetailResponse toPageDetailResponse(
          Page page
  ) {
    SurveyDetailResponse.PageDetailResponse response =
            new SurveyDetailResponse.PageDetailResponse(
            page.getId(),
            page.getTitle(),
            page.getDescription(),
            page.getOrderIndex(),
            toQuestionResponses(page.getId())
    );

    response.setMediaFiles(toMediaResponses("PAGE", page.getId()));
    return response;
  }

  private List<QuestionResponse> toQuestionResponses(UUID pageId) {
    return questionRepository.findByPage_IdOrderByOrderIndexAsc(pageId)
            .stream()
            .map(this::toQuestionResponse)
            .toList();
  }

  private QuestionResponse toQuestionResponse(Question question) {
    QuestionResponse response = new QuestionResponse(
            question.getId(),
            question.getPage().getId(),
            question.getQuestionText(),
            question.getQuestionType().getId(),
            question.getQuestionType().getCode(),
            question.getQuestionType().getName(),
            question.getIsRequired(),
            question.getOrderIndex(),
            question.getDescription(),
            toOptionResponses(question.getId())
    );

    response.setValidationRules(
            questionValidationRuleService.getByQuestion(question.getId())
    );
    response.setMediaFiles(toMediaResponses("QUESTION", question.getId()));

    return response;
  }

  private List<OptionResponse> toOptionResponses(UUID questionId) {
    return optionRepository.findByQuestion_IdOrderByOrderIndexAsc(questionId)
            .stream()
            .map(this::toOptionResponse)
            .toList();
  }

  private OptionResponse toOptionResponse(Option option) {
    OptionResponse response = new OptionResponse(
            option.getId(),
            option.getOptionText(),
            option.getOrderIndex()
    );

    response.setMediaFiles(toMediaResponses("OPTION", option.getId()));
    return response;
  }

  private List<ConditionResponse> toConditionResponses(UUID surveyId) {
    return conditionRepository
            .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                    surveyId,
                    surveyId
            )
            .stream()
            .map(this::toConditionResponse)
            .toList();
  }

  private ConditionResponse toConditionResponse(Condition condition) {
    return new ConditionResponse(
            condition.getId(),
            condition.getSourceQuestion().getId(),
            condition.getSourceValue(),
            condition.getTargetQuestion().getId(),
            condition.getAction()
    );
  }

  private SurveyResponse toResponse(Survey survey) {
    SurveyResponse response = new SurveyResponse(
            survey.getId(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getCreatorUser(),
            survey.getCreatedAt(),
            survey.getIsActive()
    );

    response.setMediaFiles(toMediaResponses("SURVEY", survey.getId()));
    response.setCreatorUserId(survey.getCreatorUserId());
    return response;
  }

  private List<MediaUploadResponse> toMediaResponses(
          String ownerType,
          UUID ownerId
  ) {
    return mediaFileRepository
            .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(ownerType, ownerId)
            .stream()
            .map(MediaUploadResponse::fromEntity)
            .toList();
  }


}
