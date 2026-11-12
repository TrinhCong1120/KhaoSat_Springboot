package com.trinhcong1120.survey_service.security;

import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.BadRequestException;
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
  private final PageRepository pages;
  private final QuestionRepository questions;
  private final OptionRepository options;
  private final ConditionRepository conditions;
  private final ResponseRepository responses;
  private final MediaFileRepository media;
  private final GroupPermissions groupPermissions;

  public SurveyAccessGuard(SurveyRepository surveys,
      PageRepository pages, QuestionRepository questions, OptionRepository options,
      ConditionRepository conditions, ResponseRepository responses,
      MediaFileRepository media, GroupPermissions groupPermissions) {
    this.surveys = surveys;
    this.pages = pages;
    this.questions = questions;
    this.options = options;
    this.conditions = conditions;
    this.responses = responses;
    this.media = media;
    this.groupPermissions = groupPermissions;
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
    if (auth == null || auth.getAuthorities().stream().noneMatch(a -> permission.equals(a.getAuthority()))) return false;
    // The migration revokes Manager's global delete grant; an old JWT must not keep it alive.
    return !"survey_delete_all".equals(permission)
        || groupPermissions.hasCurrentGlobalPermission(userId(), permission);
  }

  public Survey survey(UUID id) {
    return surveys.findById(id).orElseThrow(() -> new NotFoundException("Survey khong ton tai"));
  }

  public boolean canManageAccess(UUID id) {
    return canManageAccess(survey(id));
  }

  public record Actions(boolean canEdit, boolean canDelete, boolean canManageAccess) {}

  public Actions actions(Survey survey) {
    return new Actions(can(survey, "EDIT"), can(survey, "DELETE"), can(survey, "MANAGE_ACCESS"));
  }

  public boolean can(UUID surveyId, String permission) { return can(survey(surveyId), permission); }

  public boolean can(Survey survey, String permission) {
    if ("VIEW".equals(permission) && (has("survey_view_all") || has("survey_update_all")
        || has("survey_delete_all"))) return true;
    if (("EDIT".equals(permission) || "MANAGE_PUBLICATION".equals(permission)
        || "MANAGE_ACCESS".equals(permission)) && has("survey_update_all")) return true;
    if ("DELETE".equals(permission) && has("survey_delete_all")) return true;
    if (("VIEW_RESPONSES".equals(permission) || "EXPORT_DATA".equals(permission))
        && has("survey_view_all")) return true;
    var effective = groupPermissions.survey(survey.getId(), survey.getGroupId(), userId());
    if ("VIEW".equals(permission)) return effective.stream().anyMatch(GroupPermissions.SURVEY::contains);
    if ("VIEW_RESPONSES".equals(permission)) return effective.contains("VIEW_RESPONSES");
    return effective.contains(permission);
  }

  public void require(UUID surveyId, String permission) {
    if (!can(surveyId, permission)) throw new AccessDeniedException("Khong co quyen " + permission + " tren survey");
  }

  public void viewResponses(UUID id) { require(id, "VIEW_RESPONSES"); }
  public void exportData(UUID id) { require(id, "EXPORT_DATA"); }
  public void publication(UUID id) { require(id, "MANAGE_PUBLICATION"); }

  public java.util.Set<String> effectiveSurveyPermissions(UUID id) {
    java.util.Set<String> result = new java.util.HashSet<>();
    for (String permission : GroupPermissions.SURVEY)
      if (can(id, permission)) result.add(permission);
    return result;
  }

  public void view(UUID id) {
    require(id, "VIEW");
  }

  public void edit(UUID id) {
    require(id, "EDIT");
  }

  public void delete(UUID id) {
    require(id, "DELETE");
  }

  public void manageAccess(UUID id) {
    require(id, "MANAGE_ACCESS");
  }

  public List<Survey> visibleSurveys() {
    if (canSeeAllSurveys()) return surveys.findAllByOrderByCreatedAtDesc();
    List<UUID> ids = groupPermissions.visibleSurveyIds(userId(), false);
    return ids.isEmpty() ? List.of() : surveys.findByIdInOrderByCreatedAtDesc(ids);
  }

  public Page<Survey> visibleSurveys(Pageable pageable) {
    if (canSeeAllSurveys()) return surveys.findAll(pageable);
    List<UUID> ids = groupPermissions.visibleSurveyIds(userId(), false);
    return ids.isEmpty() ? Page.empty(pageable) : surveys.findByIdIn(ids, pageable);
  }

  public Page<Survey> visibleSurveys(Pageable pageable, UUID groupId, boolean shared) {
    if (groupId == null && !shared) return visibleSurveys(pageable);
    if (canSeeAllSurveys() && !shared) return surveys.findByGroupId(groupId, pageable);
    List<UUID> visible = canSeeAllSurveys()
        ? groupPermissions.directlySharedSurveyIds(userId())
        : groupPermissions.visibleSurveyIds(userId(), false);
    if (shared) {
      var direct = groupPermissions.directlySharedSurveyIds(userId());
      visible = visible.stream().filter(direct::contains).toList();
    }
    if (visible.isEmpty()) return Page.empty(pageable);
    return groupId == null ? surveys.findByIdIn(visible, pageable)
        : surveys.findByIdInAndGroupId(visible, groupId, pageable);
  }

  private boolean canSeeAllSurveys() {
    return has("survey_view_all") || has("survey_update_all") || has("survey_delete_all");
  }

  public void viewPage(UUID id) { view(pageSurvey(id)); }
  public void editPage(UUID id) { edit(pageSurvey(id)); }
  public void viewQuestion(UUID id) { view(questionSurvey(id)); }
  public void editQuestion(UUID id) { edit(questionSurvey(id)); }
  public void viewResponse(UUID id) { viewResponses(responses.findById(id)
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

  public void editConditionTargets(UUID sourceId, String targetType, UUID targetQuestionId, UUID targetPageId) {
    UUID sourceSurvey = questionSurvey(sourceId);
    UUID targetSurvey;
    if ("PAGE".equalsIgnoreCase(targetType)) {
      if (targetPageId == null) throw new BadRequestException("Target page id is required for PAGE condition");
      targetSurvey = pageSurvey(targetPageId);
    } else {
      if (targetQuestionId == null) throw new BadRequestException("Target question id is required for QUESTION condition");
      targetSurvey = questionSurvey(targetQuestionId);
    }
    if (!sourceSurvey.equals(targetSurvey))
      throw new AccessDeniedException("Source and target must belong to the same survey");
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
    UUID target = condition.getTargetQuestion() != null
        ? condition.getTargetQuestion().getPage().getSurvey().getId()
        : (condition.getTargetPage() != null ? condition.getTargetPage().getSurvey().getId() : source);
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

  private boolean canManageAccess(Survey survey) {
    return can(survey, "MANAGE_ACCESS");
  }

}
