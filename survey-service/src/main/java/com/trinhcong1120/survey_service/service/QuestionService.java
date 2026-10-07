package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.*;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class QuestionService {

  private final QuestionRepository questionRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final PageRepository pageRepository;
  private final QuestionTypeRepository questionTypeRepository;
  private final OptionRepository optionRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final SurveyRepository surveyRepository;
  private final MediaFileRepository mediaFileRepository;
  private final QuestionValidationRuleService questionValidationRuleService;

  public QuestionService(
          QuestionRepository questionRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          PageRepository pageRepository,
          QuestionTypeRepository questionTypeRepository,
          OptionRepository optionRepository,
          AnswerOptionRepository answerOptionRepository,
          SurveyRepository surveyRepository,
          MediaFileRepository mediaFileRepository,
          QuestionValidationRuleService questionValidationRuleService
  ) {
    this.questionRepository = questionRepository;
    this.guard = guard;
    this.pageRepository = pageRepository;
    this.questionTypeRepository = questionTypeRepository;
    this.optionRepository = optionRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.surveyRepository = surveyRepository;
    this.mediaFileRepository = mediaFileRepository;
    this.questionValidationRuleService = questionValidationRuleService;
  }

  @Transactional(readOnly = true)
  public List<QuestionResponse> getByPage(UUID pageId) {
    guard.viewPage(pageId);
    return questionRepository
            .findByPage_IdOrderByOrderIndexAsc(pageId)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  public QuestionResponse create(
          CreateQuestionRequest request
  ) {
    guard.editPage(request.getPageId());
    Page page = pageRepository.findById(request.getPageId())
            .orElseThrow(() ->
                    new NotFoundException("Page không tồn tại"));

    QuestionType type = questionTypeRepository
            .findById(request.getQuestionTypeId())
            .orElseThrow(() ->
                    new NotFoundException(
                            "Question type không tồn tại"));

    Question question = new Question();

    question.setPage(page);
    question.setQuestionText(request.getQuestionText());
    question.setQuestionType(type);
    question.setIsRequired(request.getIsRequired());
    question.setOrderIndex(request.getOrderIndex());
    question.setDescription(request.getDescription());

    question = questionRepository.save(question);

    if (isChoice(type.getCode())
            && request.getOptions() != null) {

      createOptions(question, request.getOptions());
    }

    bumpRevision(question);

    return toResponse(question);
  }

  public QuestionResponse update(
          UUID id,
          UpdateQuestionRequest request
  ) {
    guard.editQuestion(id);
    Question question = questionRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Question không tồn tại"));

    QuestionType type = questionTypeRepository
            .findById(request.getQuestionTypeId())
            .orElseThrow(() ->
                    new NotFoundException(
                            "Question type không tồn tại"));

    question.setQuestionText(request.getQuestionText());
    question.setQuestionType(type);
    question.setIsRequired(request.getIsRequired());
    question.setOrderIndex(request.getOrderIndex());
    question.setDescription(request.getDescription());

    questionRepository.save(question);

    if (isChoice(type.getCode())) {
      updateOptions(question, request.getOptions());
    } else {
      removeUnusedOptions(question);
    }

    questionRepository.save(question);
    bumpRevision(question);

    return toResponse(question);
  }

  public void delete(UUID id) {
    guard.editQuestion(id);
    Question question = questionRepository.findById(id)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Question không tồn tại"));

    List<Option> options =
            optionRepository
                    .findByQuestion_IdOrderByOrderIndexAsc(id);

    for (Option option : options) {
      if (!answerOptionRepository
              .existsByOption_Id(option.getId())) {
        optionRepository.delete(option);
      }
    }

    questionRepository.delete(question);
    bumpRevision(question);
  }

  private void bumpRevision(Question question) {
    if (question == null
            || question.getPage() == null
            || question.getPage().getSurvey() == null) {
      return;
    }

    Survey survey = question.getPage().getSurvey();
    survey.incrementValidationRevision();
    surveyRepository.save(survey);
  }

  private void createOptions(
          Question question,
          List<OptionRequest> requests
  ) {
    int index = 1;

    for (OptionRequest request : requests) {

      if (request == null
              || request.getOptionText() == null
              || request.getOptionText().isBlank()) {
        continue;
      }

      Option option = new Option();

      option.setQuestion(question);
      applyOptionRequest(option, request);
      option.setOrderIndex(index++);

      optionRepository.save(option);
    }
  }

  private void updateOptions(
          Question question,
          List<OptionRequest> newOptions
  ) {
    List<Option> oldOptions =
            optionRepository
                    .findByQuestion_IdOrderByOrderIndexAsc(
                            question.getId()
                    );

    if (newOptions == null) {
      newOptions = new ArrayList<>();
    }

    List<OptionRequest> validOptions = newOptions.stream()
            .filter(option ->
                    option != null
                            && option.getOptionText() != null
                            && !option.getOptionText().isBlank())
            .toList();

    int common =
            Math.min(oldOptions.size(), validOptions.size());

    for (int i = 0; i < common; i++) {

      Option option = oldOptions.get(i);

      applyOptionRequest(option, validOptions.get(i));
      option.setOrderIndex(i + 1);

      optionRepository.save(option);
    }

    for (int i = common; i < validOptions.size(); i++) {

      Option option = new Option();

      option.setQuestion(question);
      applyOptionRequest(option, validOptions.get(i));
      option.setOrderIndex(i + 1);

      optionRepository.save(option);
    }

    for (int i = validOptions.size();
         i < oldOptions.size();
         i++) {

      Option option = oldOptions.get(i);

      if (answerOptionRepository
              .existsByOption_Id(option.getId())) {

        if (!option.getOptionText()
                .endsWith(" (đã dùng)")) {
          option.setOptionText(
                  option.getOptionText()
                          + " (đã dùng)"
          );
        }

        optionRepository.save(option);

      } else {
        optionRepository.delete(option);
      }
    }
  }

  private void applyOptionRequest(
          Option option,
          OptionRequest request
  ) {
    option.setOptionText(request.getOptionText().trim());
  }

  private void removeUnusedOptions(Question question) {

    List<Option> options =
            optionRepository
                    .findByQuestion_IdOrderByOrderIndexAsc(
                            question.getId()
                    );

    for (Option option : options) {

      if (!answerOptionRepository
              .existsByOption_Id(option.getId())) {

        optionRepository.delete(option);
      }
    }
  }

  private boolean isChoice(String code) {
    return "SINGLE_CHOICE".equalsIgnoreCase(code)
            || "MULTIPLE_CHOICE".equalsIgnoreCase(code);
  }

  private QuestionResponse toResponse(Question question) {

    List<OptionResponse> options =
            optionRepository
                    .findByQuestion_IdOrderByOrderIndexAsc(
                            question.getId()
                    )
                    .stream()
                    .map(this::toOptionResponse)
                    .toList();

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
            options
    );

    response.setValidationRules(
            questionValidationRuleService.getByQuestion(question.getId())
    );
    response.setMediaFiles(toMediaResponses("QUESTION", question.getId()));

    return response;
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
