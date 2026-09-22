package com.trinhcong1120.survey_service.service;

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
  private final PageRepository pageRepository;
  private final QuestionTypeRepository questionTypeRepository;
  private final OptionRepository optionRepository;
  private final AnswerOptionRepository answerOptionRepository;

  public QuestionService(
          QuestionRepository questionRepository,
          PageRepository pageRepository,
          QuestionTypeRepository questionTypeRepository,
          OptionRepository optionRepository,
          AnswerOptionRepository answerOptionRepository
  ) {
    this.questionRepository = questionRepository;
    this.pageRepository = pageRepository;
    this.questionTypeRepository = questionTypeRepository;
    this.optionRepository = optionRepository;
    this.answerOptionRepository = answerOptionRepository;
  }

  @Transactional(readOnly = true)
  public List<QuestionResponse> getByPage(Integer pageId) {
    return questionRepository
            .findByPage_IdOrderByOrderIndexAsc(pageId)
            .stream()
            .map(this::toResponse)
            .toList();
  }

  public QuestionResponse create(
          CreateQuestionRequest request
  ) {
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

    return toResponse(question);
  }

  public QuestionResponse update(
          Integer id,
          UpdateQuestionRequest request
  ) {
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

    return toResponse(question);
  }

  public void delete(Integer id) {
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
  }

  private void createOptions(
          Question question,
          List<String> texts
  ) {
    int index = 1;

    for (String text : texts) {

      if (text == null || text.isBlank()) {
        continue;
      }

      Option option = new Option();

      option.setQuestion(question);
      option.setOptionText(text.trim());
      option.setOrderIndex(index++);

      optionRepository.save(option);
    }
  }

  private void updateOptions(
          Question question,
          List<String> newTexts
  ) {
    List<Option> oldOptions =
            optionRepository
                    .findByQuestion_IdOrderByOrderIndexAsc(
                            question.getId()
                    );

    if (newTexts == null) {
      newTexts = new ArrayList<>();
    }

    List<String> validTexts = newTexts.stream()
            .filter(text ->
                    text != null && !text.isBlank())
            .map(String::trim)
            .toList();

    int common =
            Math.min(oldOptions.size(), validTexts.size());

    for (int i = 0; i < common; i++) {

      Option option = oldOptions.get(i);

      option.setOptionText(validTexts.get(i));
      option.setOrderIndex(i + 1);

      optionRepository.save(option);
    }

    for (int i = common; i < validTexts.size(); i++) {

      Option option = new Option();

      option.setQuestion(question);
      option.setOptionText(validTexts.get(i));
      option.setOrderIndex(i + 1);

      optionRepository.save(option);
    }

    for (int i = validTexts.size();
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
                    .map(option ->
                            new OptionResponse(
                                    option.getId(),
                                    option.getOptionText(),
                                    option.getOrderIndex()
                            )
                    )
                    .toList();

    return new QuestionResponse(
            question.getId(),
            question.getPage().getId(),
            question.getQuestionText(),
            question.getQuestionType().getId(),
            question.getQuestionType().getCode(),
            question.getIsRequired(),
            question.getOrderIndex(),
            question.getDescription(),
            options
    );
  }
}