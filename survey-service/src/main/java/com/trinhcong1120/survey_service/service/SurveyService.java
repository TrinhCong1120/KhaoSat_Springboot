package com.trinhcong1120.survey_service.service;

import java.util.UUID;
import java.util.Optional;

import com.trinhcong1120.survey_service.dto.condition.ConditionResponse;
import com.trinhcong1120.survey_service.dto.media.MediaUploadResponse;
import com.trinhcong1120.survey_service.dto.question.OptionResponse;
import com.trinhcong1120.survey_service.dto.question.QuestionResponse;
import com.trinhcong1120.survey_service.dto.survey.*;
import com.trinhcong1120.survey_service.dto.common.PagedResponse;
import com.trinhcong1120.survey_service.entity.Condition;
import com.trinhcong1120.survey_service.entity.MediaFile;
import com.trinhcong1120.survey_service.entity.Option;
import com.trinhcong1120.survey_service.entity.Page;
import com.trinhcong1120.survey_service.entity.Question;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.entity.SurveyIntegration;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.SurveyApiException;
import com.trinhcong1120.survey_service.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.List;

@Service
@Transactional
public class SurveyService {

  private static final int PAGE_SIZE = 10;

  private static final String SHARE_CODE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom SHARE_CODE_RANDOM = new SecureRandom();

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
  private final SurveyScheduleService schedule;
  private final SurveyAlertService alerts;
  private final SurveyGroupService groups;
  private final com.trinhcong1120.survey_service.security.GroupPermissions groupPermissions;
  private final SurveyIntegrationRepository integrationRepository;

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
          QuestionValidationRuleService questionValidationRuleService,
          SurveyScheduleService schedule,
          SurveyAlertService alerts,
          SurveyGroupService groups,
          com.trinhcong1120.survey_service.security.GroupPermissions groupPermissions,
          SurveyIntegrationRepository integrationRepository
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
    this.schedule = schedule;
    this.alerts = alerts;
    this.groups = groups;
    this.groupPermissions = groupPermissions;
    this.integrationRepository = integrationRepository;
  }

  @Transactional(readOnly = true)
  public List<SurveyResponse> getAll() {
    return guard.visibleSurveys()
            .stream()
            .map(this::toResponse)
            .toList();
  }

  @Transactional(readOnly = true)
  public PagedResponse<SurveyResponse> getPage(int page, UUID groupId, boolean shared) {
    if (page < 1) throw new BadRequestException("page phai bat dau tu 1");
    var pageable = PageRequest.of(page - 1, PAGE_SIZE,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    return PagedResponse.from(guard.visibleSurveys(pageable, groupId, shared).map(this::toResponse));
  }

  public PagedResponse<SurveyResponse> getPage(int page) { return getPage(page, null, false); }

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
    response.setShareCode(survey.getShareCode());
    response.setDescription(survey.getDescription());
    response.setCreatorUser(survey.getCreatorUser());
    response.setCreatorUserId(survey.getCreatorUserId());
    response.setGroupId(survey.getGroupId());
    var actions = guard.actions(survey);
    response.setCanEdit(actions.canEdit());
    response.setCanDelete(actions.canDelete());
    response.setCanManageAccess(actions.canManageAccess());
    response.setCanViewResponses(guard.can(survey, "VIEW_RESPONSES"));
    response.setCanExportData(guard.can(survey, "EXPORT_DATA"));
    response.setCanManagePublication(guard.can(survey, "MANAGE_PUBLICATION"));
    response.setCreatedAt(survey.getCreatedAt());
    response.setStatus(schedule.status(survey).name());
    response.setOpenAt(schedule.toLocal(survey.getOpenAt()));
    response.setCloseAt(schedule.toLocal(survey.getCloseAt()));
    response.setValidationRevision(survey.getValidationRevision());
    response.setValidationRuleCatalog(questionValidationRuleService.getCatalog(null));
    response.setMediaFiles(toMediaResponses("SURVEY", survey.getId()));
    response.setPages(toPageDetailResponses(id));
    List<Condition> conditions = conditionRepository
        .findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(id, id);
    response.setConditions(conditions.stream().map(this::toConditionResponse).toList());
    response.setScheduleWarnings(conditions.stream()
        .filter(c -> c.getSourceQuestion() != null && c.getTargetQuestion() != null)
        .filter(c -> {
          Page source = c.getSourceQuestion().getPage(), target = c.getTargetQuestion().getPage();
          Instant openA = schedule.effectiveOpen(survey, source);
          Instant openB = schedule.effectiveOpen(survey, target);
          Instant closeA = schedule.effectiveClose(survey, source);
          Instant closeB = schedule.effectiveClose(survey, target);
          Instant overlapOpen = openA == null ? openB : openB == null || openA.isAfter(openB) ? openA : openB;
          Instant overlapClose = closeA == null ? closeB : closeB == null || closeA.isBefore(closeB) ? closeA : closeB;
          return overlapOpen != null && overlapClose != null && !overlapOpen.isBefore(overlapClose);
        })
        .map(c -> new SurveyDetailResponse.ScheduleWarning(c.getId(), "Lich hai trang cua condition khong giao nhau"))
        .toList());

    integrationRepository.findBySurveyId(id).ifPresent(integ -> response.setGoogleIntegration(toIntegrationDto(integ)));

    return response;
  }

  public SurveyResponse create(
          CreateSurveyRequest request,
          String username
  ) {
    if (request.getGroupId() == null) throw new BadRequestException("groupId khong duoc de trong");
    groups.require(request.getGroupId(), "CREATE_SURVEY");
    Survey survey = new Survey();
    survey.setGroupId(request.getGroupId());

    survey.setTitle(request.getTitle());
    survey.setShareCode(generateShareCode());
    survey.setDescription(request.getDescription());
    survey.setCreatorUser(username);
    survey.setCreatorUserId(guard.userId());
    survey.setCreatedAt(schedule.toLocal(schedule.now()));
    survey.setOpenAt(schedule.toInstant(request.getOpenAt()));
    survey.setCloseAt(schedule.toInstant(request.getCloseAt()));
    schedule.validate(survey.getOpenAt(), survey.getCloseAt());

    return toResponse(surveyRepository.save(survey));
  }

  public SurveyResponse update(
          UUID id,
          UpdateSurveyRequest request
  ) {
    guard.edit(id);
    Survey survey = surveyRepository.findForWrite(id).orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
    schedule.freezeIfExpired(survey, schedule.now());

    if (request.isShareCodePresent()) {
      String shareCode = request.getShareCode();
      if (shareCode == null || !shareCode.matches("[A-Za-z0-9]{4,24}"))
        throw new SurveyApiException("INVALID_SHARE_CODE", HttpStatus.BAD_REQUEST,
            "shareCode phai co 4-24 ky tu chu hoac so, khong dau va khong khoang trang");
      if (!shareCode.equals(survey.getShareCode())) {
        if (surveyRepository.existsByShareCode(shareCode))
          throw new SurveyApiException("SHARE_CODE_TAKEN", HttpStatus.CONFLICT, "shareCode da duoc su dung");
        survey.setShareCode(shareCode);
      }
    }

    survey.setTitle(request.getTitle());
    survey.setDescription(request.getDescription());
    boolean isGoogle = integrationRepository.findBySurveyId(id).isPresent();
    if (isGoogle) {
      survey.setOpenAt(null);
      survey.setCloseAt(null);
    } else {
      if (request.isOpenAtPresent()) survey.setOpenAt(schedule.toInstant(request.getOpenAt()));
      if (request.isCloseAtPresent()) survey.setCloseAt(schedule.toInstant(request.getCloseAt()));
      schedule.validate(survey.getOpenAt(), survey.getCloseAt());
      for (Page page : pageRepository.findBySurvey_IdOrderByOrderIndexAsc(id))
        schedule.validatePage(survey, page.getOpenAt(), page.getCloseAt());
      if (request.isOpenAtPresent() || request.isCloseAtPresent()) survey.incrementValidationRevision();
    }

    return toResponse(surveyRepository.save(survey));
  }

  public SurveyResponse updateStatus(
          UUID id,
          UpdateSurveyStatusRequest request
  ) {
    guard.publication(id);
    if (integrationRepository.findBySurveyId(id).isPresent()) {
      throw new BadRequestException("Khảo sát Google Form tự động đóng/mở theo Google Forms, không thể thiết lập trạng thái hoặc lên lịch thủ công.");
    }
    Survey survey = surveyRepository.findForWrite(id).orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
    Instant now = schedule.now();
    schedule.freezeIfExpired(survey, now);
    List<Page> pages = pageRepository.findBySurvey_IdOrderByOrderIndexAsc(id);
    String action = request.getAction().trim().toUpperCase();
    switch (action) {
      case "PUBLISH" -> {
        if (survey.getPublishedAt() != null) throw new BadRequestException("Survey da duoc xuat ban; dung UNLOCK neu da dong");
        if (pages.isEmpty() || questionRepository.findByPage_Survey_Id(id).isEmpty())
          throw new BadRequestException("Can it nhat mot trang co cau hoi de xuat ban");
        if (!hasPossibleQuestionPage(survey, pages, now)) throw new BadRequestException("Lich trang co cau hoi da het han");
        survey.setPublishedAt(now);
        survey.setManuallyLocked(false);
        survey.setRequiresUnlock(false);
      }
      case "LOCK" -> {
        if (survey.getPublishedAt() == null) throw new BadRequestException("Survey chua duoc xuat ban");
        survey.setManuallyLocked(true);
      }
      case "UNLOCK" -> {
        if (survey.getPublishedAt() == null) throw new BadRequestException("Survey chua duoc xuat ban");
        if (!hasPossibleQuestionPage(survey, pages, now)) throw new BadRequestException("Can thay doi lich truoc khi mo khoa");
        survey.setManuallyLocked(false);
        survey.setRequiresUnlock(false);
      }
      default -> throw new BadRequestException("Action phai la PUBLISH, LOCK hoac UNLOCK");
    }
    survey.incrementValidationRevision();

    return toResponse(surveyRepository.save(survey));
  }

  public void delete(UUID id) {
    guard.delete(id);
    Survey survey = getEntity(id);
    UUID actorId = guard.userId();
    boolean globalDelete = !actorId.equals(survey.getCreatorUserId())
        && guard.has("survey_delete_all");

    accessRepository.deleteBySurvey_Id(id);

    conditionRepository.deleteBySurveyId(id);
    answerOptionRepository.deleteBySurveyId(id);
    answerRepository.deleteBySurveyId(id);
    responseRepository.deleteBySurveyId(id);
    optionRepository.deleteBySurveyId(id);
    questionRepository.deleteBySurveyId(id);
    pageRepository.deleteBySurveyId(id);

    surveyRepository.delete(survey);
    if (globalDelete) alerts.deletedByGlobalUser(survey, actorId);
  }

  public void move(UUID id, UUID targetGroupId) {
    if (targetGroupId == null) throw new BadRequestException("groupId khong duoc de trong");
    Survey survey = getEntity(id);
    groups.require(survey.getGroupId(), "MANAGE_GROUP");
    groups.require(targetGroupId, "MANAGE_GROUP");
    survey.setGroupId(targetGroupId);
    surveyRepository.save(survey);
  }

  private boolean hasPossibleQuestionPage(Survey survey, List<Page> pages, Instant at) {
    java.util.Set<UUID> questionPageIds = questionRepository.findByPage_Survey_Id(survey.getId())
        .stream().map(q -> q.getPage().getId()).collect(java.util.stream.Collectors.toSet());
    return pages.stream().anyMatch(p -> questionPageIds.contains(p.getId())
        && schedule.canOpenLater(survey, p, at));
  }

  private String generateShareCode() {
    for (int attempt = 0; attempt < 20; attempt++) {
      StringBuilder code = new StringBuilder(8);
      for (int i = 0; i < 8; i++)
        code.append(SHARE_CODE_CHARACTERS.charAt(SHARE_CODE_RANDOM.nextInt(SHARE_CODE_CHARACTERS.length())));
      if (!surveyRepository.existsByShareCode(code.toString())) return code.toString();
    }
    throw new IllegalStateException("Khong tao duoc shareCode duy nhat");
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
    response.setOpenAt(schedule.toLocal(page.getOpenAt()));
    response.setCloseAt(schedule.toLocal(page.getCloseAt()));
    response.setEffectiveCloseAt(schedule.toLocal(schedule.effectiveClose(page.getSurvey(), page)));
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

    response.setParentId(question.getParent() != null ? question.getParent().getId() : null);
    response.setLevel(question.getLevel());
    response.setPath(question.getPath());
    response.setIsLeaf(question.getIsLeaf());
    response.setAggregationType(question.getAggregationType());
    response.setAggregationFormula(question.getAggregationFormula());
    response.setWeight(question.getWeight());
    response.setMatrixConfig(question.getMatrixConfig());

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
            condition.getSourceQuestion() != null ? condition.getSourceQuestion().getId() : null,
            condition.getSourceValue(),
            condition.getTargetType(),
            condition.getTargetQuestion() != null ? condition.getTargetQuestion().getId() : null,
            condition.getTargetPage() != null ? condition.getTargetPage().getId() : null,
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
            schedule.status(survey).name()
    );

    response.setMediaFiles(toMediaResponses("SURVEY", survey.getId()));
    response.setCreatorUserId(survey.getCreatorUserId());
    var actions = guard.actions(survey);
    response.setCanEdit(actions.canEdit());
    response.setCanDelete(actions.canDelete());
    response.setCanManageAccess(actions.canManageAccess());
    response.setGroupId(survey.getGroupId());
    response.setCanViewResponses(guard.can(survey, "VIEW_RESPONSES"));
    response.setCanExportData(guard.can(survey, "EXPORT_DATA"));
    Optional<SurveyIntegration> integOpt = integrationRepository.findBySurveyId(survey.getId());
    boolean isGoogle = integOpt.isPresent();
    response.setCanManagePublication(!isGoogle && guard.can(survey, "MANAGE_PUBLICATION"));
    response.setSharedDirectly(!groupPermissions.direct(survey.getId(), guard.userId()).isEmpty()
        && groupPermissions.group(survey.getGroupId(), guard.userId()).isEmpty());
    response.setShareCode(survey.getShareCode());
    if (isGoogle) {
      response.setOpenAt(null);
      response.setCloseAt(null);
      SurveyIntegration integ = integOpt.get();
      GoogleIntegrationDto dto = toIntegrationDto(integ);
      dto.setIsAcceptingResponses(!survey.isManuallyLocked());
      response.setGoogleIntegration(dto);
    } else {
      response.setOpenAt(schedule.toLocal(survey.getOpenAt()));
      response.setCloseAt(schedule.toLocal(survey.getCloseAt()));
    }
    return response;
  }

  private GoogleIntegrationDto toIntegrationDto(SurveyIntegration integ) {
    GoogleIntegrationDto dto = new GoogleIntegrationDto();
    dto.setId(integ.getId());
    dto.setProvider(integ.getProvider());
    dto.setGoogleFormId(integ.getGoogleFormId());
    dto.setEditUrl(integ.getEditUrl());
    dto.setResponseUrl(integ.getResponseUrl());
    dto.setSyncStatus(integ.getSyncStatus());
    dto.setLastSyncedAt(integ.getLastSyncedAt());
    dto.setLastSyncError(integ.getLastSyncError());
    return dto;
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
