package com.trinhcong1120.survey_service.security;

import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.*;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional(readOnly = true)
public class SurveyAccessGuard {
  private final SurveyRepository surveys;
  private final SurveyAccessRepository accesses;
  private final PageRepository pages;
  private final QuestionRepository questions;
  private final OptionRepository options;
  private final ConditionRepository conditions;
  private final ResponseRepository responses;
  private final MediaFileRepository media;

  public SurveyAccessGuard(SurveyRepository surveys, SurveyAccessRepository accesses,
      PageRepository pages, QuestionRepository questions, OptionRepository options,
      ConditionRepository conditions, ResponseRepository responses,
      MediaFileRepository media) {
    this.surveys = surveys;
    this.accesses = accesses;
    this.pages = pages;
    this.questions = questions;
    this.options = options;
    this.conditions = conditions;
    this.responses = responses;
    this.media = media;
  }

  public UUID userId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new AccessDeniedException("Authentication required");
    }
    try {
      return UUID.fromString(auth.getName());
    } catch (IllegalArgumentException ex) {
      throw new AccessDeniedException("Invalid user ID");
    }
  }

  public boolean has(String permission) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> permission.equals(a.getAuthority()));
  }

  public Survey survey(UUID id) {
    return surveys.findById(id).orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
  }

  public boolean canManageAccess(UUID id) {
    return canManageAccess(survey(id));
  }

  public record Actions(boolean canEdit, boolean canDelete, boolean canManageAccess) {}

  public Actions actions(Survey survey) {
    boolean owner = isOwner(survey);
    boolean edit = has("survey_update_all");
    boolean delete = has("survey_delete_all");
    if (edit && delete) return new Actions(true, true, true);
    boolean canEdit = has("survey_update") && (owner || access(survey).map(SurveyAccess::isCanEdit).orElse(false));
    return new Actions(edit || canEdit, delete || (has("survey_delete") && owner),
        edit || (has("survey_update") && owner));
  }

  public void view(UUID id) {
    Survey survey = survey(id);
    if (has("survey_view_all")) return;
    if (has("survey_view") && (isOwner(survey) || access(survey).map(SurveyAccess::isCanView).orElse(false))) return;
    throw new AccessDeniedException("Khong co quyen xem survey");
  }

  public void edit(UUID id) {
    Survey survey = survey(id);
    if (has("survey_update_all")) return;
    if (has("survey_update") && (isOwner(survey) || access(survey).map(SurveyAccess::isCanEdit).orElse(false))) return;
    throw new AccessDeniedException("Khong co quyen sua survey");
  }

  public void delete(UUID id) {
    Survey survey = survey(id);
    if (has("survey_delete_all") || (has("survey_delete") && isOwner(survey))) return;
    throw new AccessDeniedException("Khong co quyen xoa survey");
  }

  public void manageAccess(UUID id) {
    Survey survey = survey(id);
    if (canManageAccess(survey)) return;
    throw new AccessDeniedException("Khong co quyen quan ly access");
  }

  public List<Survey> visibleSurveys() {
    if (has("survey_view_all"))
      return surveys.findAllByOrderByCreatedAtDesc();
    if (!has("survey_view")) return List.of();
    return surveys.findVisibleTo(userId());
  }

  public Page<Survey> visibleSurveys(Pageable pageable) {
    if (has("survey_view_all"))
      return surveys.findAll(pageable);
    if (!has("survey_view")) return Page.empty(pageable);
    return surveys.findVisibleTo(userId(), pageable);
  }

  public void viewPage(UUID id) { view(pageSurvey(id)); }
  public void editPage(UUID id) { edit(pageSurvey(id)); }
  public void viewQuestion(UUID id) { view(questionSurvey(id)); }
  public void editQuestion(UUID id) { edit(questionSurvey(id)); }
  public void viewResponse(UUID id) { view(responses.findById(id)
      .orElseThrow(() -> new NotFoundException("Response khong ton tai"))
      .getSurvey().getId()); }

  public void viewCondition(UUID id) { view(conditionSurvey(id)); }
  public void editCondition(UUID id) { edit(conditionSurvey(id)); }

  public void editConditionQuestions(UUID sourceId, UUID targetId) {
    UUID sourceSurvey = questionSurvey(sourceId);
    UUID targetSurvey = questionSurvey(targetId);
    if (!sourceSurvey.equals(targetSurvey))
      throw new AccessDeniedException("Questions phai cung survey");
    edit(sourceSurvey);
  }

  public void viewMediaOwner(String type, UUID id) { view(mediaSurvey(type, id)); }
  public void editMediaOwner(String type, UUID id) { edit(mediaSurvey(type, id)); }
  public void editMedia(UUID id) {
    MediaFile file = media.findById(id).orElseThrow(() -> new NotFoundException("Media khong ton tai"));
    editMediaOwner(file.getOwnerType(), file.getOwnerId());
  }

  public UUID pageSurvey(UUID id) {
    return pages.findById(id).orElseThrow(() -> new NotFoundException("Page khong ton tai"))
        .getSurvey().getId();
  }

  public UUID questionSurvey(UUID id) {
    return questions.findById(id).orElseThrow(() -> new NotFoundException("Question khong ton tai"))
        .getPage().getSurvey().getId();
  }

  private UUID conditionSurvey(UUID id) {
    Condition condition = conditions.findById(id)
        .orElseThrow(() -> new NotFoundException("Condition khong ton tai"));
    UUID source = condition.getSourceQuestion().getPage().getSurvey().getId();
    UUID target = condition.getTargetQuestion().getPage().getSurvey().getId();
    if (!source.equals(target)) throw new AccessDeniedException("Condition qua nhieu survey");
    return source;
  }

  private UUID mediaSurvey(String type, UUID id) {
    return switch (type == null ? "" : type.toUpperCase()) {
      case "SURVEY" -> survey(id).getId();
      case "PAGE" -> pageSurvey(id);
      case "QUESTION" -> questionSurvey(id);
      case "OPTION" -> options.findById(id)
          .orElseThrow(() -> new NotFoundException("Option khong ton tai"))
          .getQuestion().getPage().getSurvey().getId();
      default -> throw new NotFoundException("Media owner khong ton tai");
    };
  }

  private boolean isOwner(Survey survey) {
    return userId().equals(survey.getCreatorUserId());
  }

  private boolean canManageAccess(Survey survey) {
    return has("survey_update_all") || (has("survey_update") && isOwner(survey));
  }

  private java.util.Optional<SurveyAccess> access(Survey survey) {
    return accesses.findBySurvey_IdAndUserId(survey.getId(), userId());
  }
}
