package com.trinhcong1120.survey_service.service;

import java.util.UUID;

import com.trinhcong1120.survey_service.dto.common.PagedResponse;
import com.trinhcong1120.survey_service.dto.filter.ResponseFilterRequest;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseDetailResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseListResponse;
import com.trinhcong1120.survey_service.dto.response.ResponsePageResponse;
import com.trinhcong1120.survey_service.dto.response.ResponseQuestionResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.entity.Response;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.util.AnswerValueUtil;
import com.trinhcong1120.survey_service.util.ConditionUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResponseService {

  private static final int PAGE_SIZE = 10;

  private final ResponseRepository responseRepository;
  private final com.trinhcong1120.survey_service.security.SurveyAccessGuard guard;
  private final AnswerRepository answerRepository;
  private final AnswerOptionRepository answerOptionRepository;
  private final PageRepository pageRepository;
  private final QuestionRepository questionRepository;
  private final ConditionRepository conditionRepository;
  private final MediaFileRepository mediaFileRepository;

  public ResponseService(
          ResponseRepository responseRepository,
          com.trinhcong1120.survey_service.security.SurveyAccessGuard guard,
          AnswerRepository answerRepository,
          AnswerOptionRepository answerOptionRepository,
          PageRepository pageRepository,
          QuestionRepository questionRepository,
          ConditionRepository conditionRepository,
          MediaFileRepository mediaFileRepository
  ) {
    this.responseRepository = responseRepository;
    this.guard = guard;
    this.answerRepository = answerRepository;
    this.answerOptionRepository = answerOptionRepository;
    this.pageRepository = pageRepository;
    this.questionRepository = questionRepository;
    this.conditionRepository = conditionRepository;
    this.mediaFileRepository = mediaFileRepository;
  }

  public PagedResponse<ResponseListResponse> getBySurvey(
          UUID surveyId,
          int page,
          String query
  ) {
    guard.view(surveyId);
    return findPage(surveyId, null, query, page);
  }

  public PagedResponse<ResponseListResponse> filter(
          UUID surveyId,
          ResponseFilterRequest request,
          int page,
          String query
  ) {
    guard.view(surveyId);
    return findPage(surveyId, request, query, page);
  }

  private PagedResponse<ResponseListResponse> findPage(
          UUID surveyId,
          ResponseFilterRequest filter,
          String query,
          int page
  ) {
    if (page < 1) throw new BadRequestException("page phai bat dau tu 1");
    org.springframework.data.domain.Page<ResponseListResponse> result = responseRepository.findAll(
            responseSpecification(surveyId, filter, query),
            PageRequest.of(
                    page - 1,
                    PAGE_SIZE,
                    Sort.by(
                            Sort.Order.desc("submittedAt"),
                            Sort.Order.desc("id")
                    )
            )
    ).map(this::toListResponse);
    return PagedResponse.from(result);
  }

  private Specification<Response> responseSpecification(
          UUID surveyId,
          ResponseFilterRequest filter,
          String queryText
  ) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("survey").get("id"), surveyId));

      if (filter != null) {
        if (filter.getFrom() != null) {
          predicates.add(builder.greaterThanOrEqualTo(
                  root.get("submittedAt"),
                  filter.getFrom().toInstant(ZoneOffset.UTC)
          ));
        }
        if (filter.getTo() != null) {
          predicates.add(builder.lessThanOrEqualTo(
                  root.get("submittedAt"),
                  filter.getTo().toInstant(ZoneOffset.UTC)
          ));
        }
        if (filter.getQuestionId() != null) {
          predicates.add(answerFilterPredicate(root, query, builder, filter));
        }
      }

      String search = normalize(queryText);
      if (search != null) {
        String pattern = "%" + search + "%";
        Subquery<Integer> answerSearch = query.subquery(Integer.class);
        Root<Answer> answer = answerSearch.from(Answer.class);
        answerSearch.select(builder.literal(1));
        answerSearch.where(
                builder.equal(answer.get("response"), root),
                builder.or(
                        contains(builder, answer.get("answerText"), pattern),
                        contains(builder, asString(answer.get("answerNumber")), pattern),
                        contains(builder, asString(answer.get("answerDate")), pattern),
                        contains(builder, answer.get("province"), pattern),
                        contains(builder, answer.get("ward"), pattern),
                        contains(builder, answer.get("addressDetail"), pattern)
                )
        );
        predicates.add(builder.or(
                contains(builder, asString(root.get("requestId")), pattern),
                builder.exists(answerSearch)
        ));
      }

      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private Predicate answerFilterPredicate(
          Root<Response> response,
          jakarta.persistence.criteria.CriteriaQuery<?> query,
          jakarta.persistence.criteria.CriteriaBuilder builder,
          ResponseFilterRequest filter
  ) {
    Subquery<Integer> answerFilter = query.subquery(Integer.class);
    Root<Answer> answer = answerFilter.from(Answer.class);
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.equal(answer.get("response"), response));
    predicates.add(builder.equal(answer.get("question").get("id"), filter.getQuestionId()));

    if (filter.getOptionId() != null) {
      Subquery<Integer> optionFilter = answerFilter.subquery(Integer.class);
      Root<AnswerOption> answerOption = optionFilter.from(AnswerOption.class);
      optionFilter.select(builder.literal(1));
      optionFilter.where(
              builder.equal(answerOption.get("answer"), answer),
              builder.equal(answerOption.get("option").get("id"), filter.getOptionId())
      );
      predicates.add(builder.exists(optionFilter));
    }
    String text = normalize(filter.getText());
    if (text != null) {
      predicates.add(contains(builder, answer.get("answerText"), "%" + text + "%"));
    }
    if (filter.getNumber() != null) {
      predicates.add(builder.equal(answer.get("answerNumber"), filter.getNumber()));
    }
    if (filter.getDate() != null) {
      predicates.add(builder.equal(
              builder.function("date", LocalDate.class, answer.get("answerDate")),
              filter.getDate()
      ));
    }
    String province = normalize(filter.getProvince());
    if (province != null) {
      predicates.add(builder.equal(
              builder.lower(builder.trim(answer.get("province"))),
              province
      ));
    }
    String ward = normalize(filter.getWard());
    if (ward != null) {
      predicates.add(builder.equal(
              builder.lower(builder.trim(answer.get("ward"))),
              ward
      ));
    }

    answerFilter.select(builder.literal(1));
    answerFilter.where(predicates.toArray(Predicate[]::new));
    return builder.exists(answerFilter);
  }

  private Predicate contains(
          jakarta.persistence.criteria.CriteriaBuilder builder,
          jakarta.persistence.criteria.Expression<String> expression,
          String pattern
  ) {
    return builder.like(builder.lower(builder.coalesce(expression, "")), pattern);
  }

  private jakarta.persistence.criteria.Expression<String> asString(
          jakarta.persistence.criteria.Expression<?> expression
  ) {
    return ((JpaExpression<?>) expression).cast(String.class);
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  public ResponseDetailResponse getDetail(
          UUID responseId
  ) {
    guard.viewResponse(responseId);
    Response response =
            responseRepository
                    .findById(responseId)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "Response not found"));

    UUID surveyId =
            response.getSurvey().getId();

    List<Page> pages =
            pageRepository
                    .findBySurvey_IdOrderByOrderIndexAsc(
                            surveyId
                    );

    List<Question> questions =
            questionRepository
                    .findByPage_Survey_Id(surveyId);

    List<Answer> answers =
            answerRepository
                    .findByResponse_Id(response.getId());

    for (Answer answer : answers) {
      answer.setAnswerOptions(
              answerOptionRepository
                      .findByAnswer_Id(answer.getId())
      );
    }

    List<Condition> conditions =
            conditionRepository
                    .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
                            surveyId,
                            surveyId
                    );

    Map<UUID, Answer> answerMap =
            answers.stream()
                    .filter(answer ->
                            answer.getQuestion() != null)
                    .collect(Collectors.toMap(
                            answer -> answer
                                    .getQuestion()
                                    .getId(),
                            answer -> answer,
                            (first, second) -> first
                    ));

    Map<UUID, Boolean> applicability =
            ConditionUtil.calculateApplicability(
                    questions,
                    answers,
                    conditions
            );

    List<ResponsePageResponse> pageResponses =
            pages.stream()
                    .map(page ->
                            toPageResponse(
                                    page,
                                    answerMap,
                                    applicability
                            )
                    )
                    .toList();

    ResponseDetailResponse detail = new ResponseDetailResponse(
            response.getId(),
            surveyId,
            response.getSurvey().getTitle(),
            response.getSurvey().getDescription(),
            response.getRequestId(),
            response.getSubmittedAt(),
            pageResponses
    );
    detail.setMediaFiles(toMediaResponses("SURVEY", surveyId));
    return detail;
  }

  public Response getEntity(UUID responseId) {
    return responseRepository
            .findById(responseId)
            .orElseThrow(() ->
                    new NotFoundException(
                            "Response khong ton tai"));
  }

  private List<MediaUploadResponse> toMediaResponses(String ownerType, UUID ownerId) {
    return mediaFileRepository
            .findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(ownerType, ownerId)
            .stream()
            .map(MediaUploadResponse::fromEntity)
            .toList();
  }

  private ResponsePageResponse toPageResponse(
          Page page,
          Map<UUID, Answer> answerMap,
          Map<UUID, Boolean> applicability
  ) {
    List<Question> questions =
            questionRepository
                    .findByPage_IdOrderByOrderIndexAsc(
                            page.getId()
                    );

    ResponsePageResponse result = new ResponsePageResponse(
            page.getId(),
            page.getTitle(),
            page.getDescription(),
            page.getOrderIndex(),
            questions.stream()
                    .map(question ->
                            toQuestionResponse(
                                    question,
                                    answerMap.get(question.getId()),
                                    applicability.getOrDefault(
                                            question.getId(),
                                            true
                                    )
                            )
                    )
                    .toList()
    );
    result.setMediaFiles(toMediaResponses("PAGE", page.getId()));
    return result;
  }

  private ResponseQuestionResponse toQuestionResponse(
          Question question,
          Answer answer,
          Boolean isApplicable
  ) {
    ResponseQuestionResponse result =
            new ResponseQuestionResponse();

    result.setQuestionId(question.getId());
    result.setQuestionText(question.getQuestionText());
    result.setDescription(question.getDescription());
    result.setMediaFiles(toMediaResponses("QUESTION", question.getId()));
    result.setQuestionTypeId(
            question.getQuestionType() == null
                    ? null
                    : question.getQuestionType().getId()
    );
    result.setQuestionTypeCode(
            question.getQuestionType() == null
                    ? null
                    : question.getQuestionType().getCode()
    );
    result.setIsRequired(question.getIsRequired());
    result.setIsApplicable(isApplicable);

    if (answer == null) {
      return result;
    }

    result.setAnswerText(answer.getAnswerText());
    result.setAnswerNumber(answer.getAnswerNumber());
    result.setAnswerDate(answer.getAnswerDate());
    result.setProvinceCode(answer.getProvinceCode());
    result.setWardCode(answer.getWardCode());
    result.setProvince(answer.getProvince());
    result.setWard(answer.getWard());
    result.setAddressDetail(answer.getAddressDetail());
    result.setFormattedAnswer(
            AnswerValueUtil.formatAnswer(answer)
    );

    List<AnswerOption> answerOptions =
            answer.getAnswerOptions() == null
                    ? List.of()
                    : answer.getAnswerOptions();

    result.setOptionIds(
            answerOptions.stream()
                    .map(AnswerOption::getOption)
                    .filter(Objects::nonNull)
                    .map(Option::getId)
                    .toList()
    );

    result.setOptionTexts(
            answerOptions.stream()
                    .map(AnswerOption::getOption)
                    .filter(Objects::nonNull)
                    .map(Option::getOptionText)
                    .filter(Objects::nonNull)
                    .toList()
    );

    return result;
  }

  private ResponseListResponse toListResponse(
          Response response
  ) {
    List<Answer> answers =
            answerRepository
                    .findByResponse_Id(response.getId());

    for (Answer answer : answers) {
      answer.setAnswerOptions(
              answerOptionRepository
                      .findByAnswer_Id(answer.getId())
      );
    }

    String preview = answers.stream()
            .sorted(Comparator.comparing(answer ->
                    answer.getQuestion() == null
                            ? Integer.MAX_VALUE
                            : answer.getQuestion().getOrderIndex()))
            .filter(answer ->
                    !isChoice(answer))
            .map(AnswerValueUtil::formatAnswer)
            .filter(value ->
                    value != null && !value.isBlank())
            .limit(3)
            .collect(Collectors.joining(" | "));

    return new ResponseListResponse(
            response.getId(),
            response.getSurvey().getId(),
            response.getRequestId(),
            response.getSubmittedAt(),
            preview
    );
  }

  private boolean isChoice(Answer answer) {
    if (answer == null
            || answer.getQuestion() == null
            || answer.getQuestion()
            .getQuestionType() == null
            || answer.getQuestion()
            .getQuestionType()
            .getCode() == null) {
      return false;
    }

    String type =
            answer.getQuestion()
                    .getQuestionType()
                    .getCode();

    return "SINGLE_CHOICE".equalsIgnoreCase(type)
            || "MULTIPLE_CHOICE".equalsIgnoreCase(type);
  }

}
