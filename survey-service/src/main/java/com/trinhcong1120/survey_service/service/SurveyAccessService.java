package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.entity.SurveyAccess;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.SurveyAccessRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SurveyAccessService {
  public record AccessRequest(List<String> permissions) {}
  public record AccessResponse(UUID userId, String username, String email, List<String> permissions) {}
  public record OwnerRequest(UUID userId) {}
  private record UserIdentity(String username, String email) {}

  private final SurveyAccessGuard guard;
  private final SurveyRepository surveys;
  private final SurveyAccessRepository accesses;
  private final JdbcTemplate jdbc;
  private final SurveyAlertService alerts;

  public SurveyAccessService(SurveyAccessGuard guard, SurveyRepository surveys,
      SurveyAccessRepository accesses, JdbcTemplate jdbc, SurveyAlertService alerts) {
    this.guard = guard;
    this.surveys = surveys;
    this.accesses = accesses;
    this.jdbc = jdbc;
    this.alerts = alerts;
  }

  @Transactional(readOnly = true)
  public List<AccessResponse> list(UUID surveyId) {
    guard.manageAccess(surveyId);
    return jdbc.query(
        "SELECT a.user_id, u.username, u.email, a.can_edit "
            + "FROM survey_access a LEFT JOIN users u ON u.id = a.user_id "
            + "WHERE a.survey_id = ? ORDER BY a.user_id",
        (rs, rowNum) -> new AccessResponse(
            rs.getObject("user_id", UUID.class),
            rs.getString("username"),
            maskEmail(rs.getString("email")),
            rs.getBoolean("can_edit") ? List.of("VIEW", "EDIT") : List.of("VIEW")),
        surveyId);
  }

  public AccessResponse put(UUID surveyId, UUID userId, AccessRequest request) {
    guard.manageAccess(surveyId);
    Survey survey = guard.survey(surveyId);
    UserIdentity user = activeUser(userId);
    if (userId.equals(survey.getCreatorUserId()))
      throw new BadRequestException("Owner khong can survey access");
    List<String> permissions = request == null ? null : request.permissions();
    if (permissions == null || permissions.isEmpty() ||
        permissions.stream().anyMatch(p -> !"VIEW".equals(p) && !"EDIT".equals(p)))
      throw new BadRequestException("Permissions chi gom VIEW va EDIT");

    SurveyAccess access = accesses.findBySurvey_IdAndUserId(surveyId, userId)
        .orElseGet(SurveyAccess::new);
    access.setSurvey(survey);
    access.setUserId(userId);
    access.setCanView(true);
    access.setCanEdit(permissions.contains("EDIT"));
    return response(accesses.save(access), user);
  }

  public void remove(UUID surveyId, UUID userId) {
    guard.manageAccess(surveyId);
    accesses.deleteBySurvey_IdAndUserId(surveyId, userId);
  }

  public void changeOwner(UUID surveyId, OwnerRequest request) {
    guard.manageAccess(surveyId);
    Survey survey = guard.survey(surveyId);
    UUID previousOwnerId = survey.getCreatorUserId();
    UUID actorId = guard.userId();
    boolean globalChange = !actorId.equals(previousOwnerId) && guard.has("survey_update_all");
    UUID userId = request == null ? null : request.userId();
    UserIdentity user = activeUser(userId);
    if (userId.equals(previousOwnerId)) return;
    accesses.deleteBySurvey_IdAndUserId(surveyId, userId);
    if (previousOwnerId != null) {
      SurveyAccess previousOwnerAccess = accesses.findBySurvey_IdAndUserId(surveyId, previousOwnerId)
          .orElseGet(SurveyAccess::new);
      previousOwnerAccess.setSurvey(survey);
      previousOwnerAccess.setUserId(previousOwnerId);
      previousOwnerAccess.setCanView(true);
      previousOwnerAccess.setCanEdit(true);
      accesses.save(previousOwnerAccess);
    }
    survey.setCreatorUserId(userId);
    survey.setCreatorUser(user.username());
    surveys.save(survey);
    if (globalChange) alerts.ownerChangedByGlobalUser(survey, actorId, previousOwnerId, userId);
  }

  private UserIdentity activeUser(UUID userId) {
    if (userId == null) throw new BadRequestException("userId khong duoc de trong");
    return jdbc.query(
        "SELECT username, email FROM users WHERE id = ? AND is_active IS TRUE",
        (rs, rowNum) -> new UserIdentity(rs.getString("username"), rs.getString("email")),
        userId).stream().findFirst()
        .orElseThrow(() -> new NotFoundException("User dang hoat dong khong ton tai"));
  }

  private AccessResponse response(SurveyAccess a, UserIdentity user) {
    return new AccessResponse(a.getUserId(), user.username(), maskEmail(user.email()),
        a.isCanEdit() ? List.of("VIEW", "EDIT") : List.of("VIEW"));
  }

  private String maskEmail(String email) {
    if (email == null) return null;
    int at = email.indexOf('@');
    if (at <= 0 || at == email.length() - 1) return null;
    int visible = Math.min(4, at - 1);
    return email.substring(0, visible) + "****" + email.substring(at);
  }
}
