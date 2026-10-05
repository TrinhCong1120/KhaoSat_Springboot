package com.trinhcong1120.survey_service.service;

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
    return surveyRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public Survey getEntity(Integer id) {
    return surveyRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException("Survey khong ton tai"));
  }

  @Transactional(readOnly = true)
  public SurveyDetailResponse getDetail(Integer id) {
    Survey survey = getEntity(id);

    SurveyDetailResponse response = new SurveyDetailResponse();
    response.setId(survey.getId());
    response.setTitle(survey.getTitle());
    response.setDescription(survey.getDescription());
    response.setImageUrl(survey.getImageUrl());
    response.setVideoUrl(survey.getVideoUrl());
    response.setAudioUrl(survey.getAudioUrl());
    response.setCreatorUser(survey.getCreatorUser());
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
    survey.setImageUrl(request.getImageUrl());
    survey.setVideoUrl(request.getVideoUrl());
    survey.setAudioUrl(request.getAudioUrl());
    survey.setCreatorUser(username);
    survey.setCreatedAt(LocalDateTime.now());
    survey.setIsActive(false);

    return toResponse(surveyRepository.save(survey));
  }

  public SurveyResponse update(
          Integer id,
          UpdateSurveyRequest request
  ) {
    Survey survey = getEntity(id);

    survey.setTitle(request.getTitle());
    survey.setDescription(request.getDescription());
    survey.setImageUrl(request.getImageUrl());
    survey.setVideoUrl(request.getVideoUrl());
    survey.setAudioUrl(request.getAudioUrl());

    return toResponse(surveyRepository.save(survey));
  }

  public SurveyResponse updateStatus(
          Integer id,
          UpdateSurveyStatusRequest request
  ) {
    Survey survey = getEntity(id);

    survey.setIsActive(request.getIsActive());

    return toResponse(surveyRepository.save(survey));
  }

  public void delete(Integer id) {
    Survey survey = getEntity(id);

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
          Integer surveyId
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
            page.getImageUrl(),
            page.getVideoUrl(),
            page.getAudioUrl(),
            page.getOrderIndex(),
            toQuestionResponses(page.getId())
    );

    response.setMediaFiles(toMediaResponses("PAGE", page.getId()));
    return response;
  }

  private List<QuestionResponse> toQuestionResponses(Integer pageId) {
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
            question.getImageUrl(),
            question.getVideoUrl(),
            question.getAudioUrl(),
            toOptionResponses(question.getId())
    );

    response.setValidationRules(
            questionValidationRuleService.getByQuestion(question.getId())
    );
    response.setMediaFiles(toMediaResponses("QUESTION", question.getId()));

    return response;
  }

  private List<OptionResponse> toOptionResponses(Integer questionId) {
    return optionRepository.findByQuestion_IdOrderByOrderIndexAsc(questionId)
            .stream()
            .map(this::toOptionResponse)
            .toList();
  }

  private OptionResponse toOptionResponse(Option option) {
    OptionResponse response = new OptionResponse(
            option.getId(),
            option.getOptionText(),
            option.getImageUrl(),
            option.getVideoUrl(),
            option.getAudioUrl(),
            option.getOrderIndex()
    );

    response.setMediaFiles(toMediaResponses("OPTION", option.getId()));
    return response;
  }

  private List<ConditionResponse> toConditionResponses(Integer surveyId) {
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
            survey.getImageUrl(),
            survey.getVideoUrl(),
            survey.getAudioUrl(),
            survey.getCreatorUser(),
            survey.getCreatedAt(),
            survey.getIsActive()
    );

    response.setMediaFiles(toMediaResponses("SURVEY", survey.getId()));
    return response;
  }

  private List<MediaUploadResponse> toMediaResponses(
          String ownerType,
          Integer ownerId
  ) {
    return mediaFileRepository
            .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(ownerType, ownerId)
            .stream()
            .map(this::toMediaResponse)
            .toList();
  }

  private MediaUploadResponse toMediaResponse(MediaFile mediaFile) {
    MediaUploadResponse response = new MediaUploadResponse();
    response.setId(mediaFile.getId());
    response.setOwnerType(mediaFile.getOwnerType());
    response.setOwnerId(mediaFile.getOwnerId());
    response.setMediaType(mediaFile.getMediaType());
    response.setOriginalFilename(mediaFile.getOriginalFilename());
    response.setContentType(mediaFile.getContentType());
    response.setSizeBytes(mediaFile.getSizeBytes());
    response.setBucketName(mediaFile.getBucketName());
    response.setObjectKey(mediaFile.getObjectKey());
    response.setObjectUrl(mediaFile.getObjectUrl());
    response.setCreatedByUserId(mediaFile.getCreatedByUserId());
    response.setUploadedAt(mediaFile.getUploadedAt());
    return response;
  }
}
