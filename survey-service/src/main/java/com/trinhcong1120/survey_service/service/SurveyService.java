package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.question.OptionResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;
import com.trinhcong1120.survey_service.dto.survey.*;
import com.trinhcong1120.survey_service.entity.Condition;
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

  public SurveyService(
          SurveyRepository surveyRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          OptionRepository optionRepository,
          ConditionRepository conditionRepository
  ) {
    this.surveyRepository = surveyRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.optionRepository = optionRepository;
    this.conditionRepository = conditionRepository;
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
    response.setCreatorUser(survey.getCreatorUser());
    response.setCreatedAt(survey.getCreatedAt());
    response.setIsActive(survey.getIsActive());
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
    return new SurveyDetailResponse.PageDetailResponse(
            page.getId(),
            page.getTitle(),
            page.getOrderIndex(),
            toQuestionResponses(page.getId())
    );
  }

  private List<QuestionResponse> toQuestionResponses(Integer pageId) {
    return questionRepository.findByPage_IdOrderByOrderIndexAsc(pageId)
            .stream()
            .map(this::toQuestionResponse)
            .toList();
  }

  private QuestionResponse toQuestionResponse(Question question) {
    return new QuestionResponse(
            question.getId(),
            question.getPage().getId(),
            question.getQuestionText(),
            question.getQuestionType().getId(),
            question.getQuestionType().getCode(),
            question.getIsRequired(),
            question.getOrderIndex(),
            question.getDescription(),
            toOptionResponses(question.getId())
    );
  }

  private List<OptionResponse> toOptionResponses(Integer questionId) {
    return optionRepository.findByQuestion_IdOrderByOrderIndexAsc(questionId)
            .stream()
            .map(option ->
                    new OptionResponse(
                            option.getId(),
                            option.getOptionText(),
                            option.getOrderIndex()
                    )
            )
            .toList();
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
    return new SurveyResponse(
            survey.getId(),
            survey.getTitle(),
            survey.getDescription(),
            survey.getCreatorUser(),
            survey.getCreatedAt(),
            survey.getIsActive()
    );
  }
}
