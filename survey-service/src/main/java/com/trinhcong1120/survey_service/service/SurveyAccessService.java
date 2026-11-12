package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SurveyAccessService {
  public record AccessRequest(List<String> permissions) {}
  public record AccessResponse(UUID userId, String username, String email, List<String> permissions) {}
  public record OwnerRequest(UUID userId) {}
  public record UserRow(UUID id, String username, String email) {}
  private record UserIdentity(String username, String email) {}
  private final SurveyAccessGuard guard;
  private final SurveyRepository surveys;
  private final JdbcTemplate jdbc;
  private final GroupPermissions groupPermissions;
  private final SurveyAlertService alerts;

  public SurveyAccessService(SurveyAccessGuard guard, SurveyRepository surveys,
      JdbcTemplate jdbc, SurveyAlertService alerts, GroupPermissions groupPermissions) {
    this.guard = guard;
    this.surveys = surveys;
    this.jdbc = jdbc;
    this.alerts = alerts;
    this.groupPermissions = groupPermissions;
  }

  @Transactional(readOnly = true)
  public List<AccessResponse> list(UUID surveyId) {
    guard.manageAccess(surveyId);
    return jdbc.query("SELECT a.user_id,u.username,u.email,a.permission FROM survey_access_permissions a "
        + "JOIN users u ON u.id=a.user_id WHERE a.survey_id=? ORDER BY a.user_id,a.permission", rs -> {
          List<AccessResponse> rows = new ArrayList<>();
          while (rs.next()) {
            UUID userId = rs.getObject(1, UUID.class);
            AccessResponse old = rows.isEmpty() ? null : rows.get(rows.size()-1);
            if (old == null || !old.userId().equals(userId)) {
              old = new AccessResponse(userId, rs.getString(2), maskEmail(rs.getString(3)), new ArrayList<>());
              rows.add(old);
            }
            old.permissions().add(rs.getString(4));
          }
          return rows;
        }, surveyId);
  }

  @Transactional(readOnly = true)
  public List<UserRow> searchUsers(UUID surveyId, String query) {
    guard.manageAccess(surveyId);
    if (query == null || query.isBlank() || query.length() > 255) throw new BadRequestException("Tu khoa khong hop le");
    return jdbc.query("SELECT id,username,email FROM users WHERE is_active IS TRUE "
        + "AND (lower(username)=lower(?) OR lower(email)=lower(?)) LIMIT 10",
        (rs, n) -> new UserRow(rs.getObject(1, UUID.class), rs.getString(2), maskEmail(rs.getString(3))),
        query.strip(), query.strip());
  }

  public AccessResponse put(UUID surveyId, UUID userId, AccessRequest request) {
    guard.manageAccess(surveyId);
    UserIdentity user = activeUser(userId);
    Set<String> wanted = new HashSet<>(request == null || request.permissions() == null ? List.of() : request.permissions());
    if (wanted.isEmpty() || !GroupPermissions.SURVEY.containsAll(wanted))
      throw new BadRequestException("Permissions khong hop le");
    Set<String> existing = new HashSet<>(groupPermissions.direct(surveyId, userId));
    Set<String> changed = new HashSet<>(wanted);
    changed.removeAll(existing);
    existing.removeAll(wanted);
    changed.addAll(existing);
    for (String permission : changed)
      if (!guard.can(surveyId, permission)) throw new AccessDeniedException("Khong duoc thay doi quyen vuot quyen hien co");
    jdbc.update("DELETE FROM survey_access_permissions WHERE survey_id=? AND user_id=?", surveyId, userId);
    wanted.forEach(p -> jdbc.update("INSERT INTO survey_access_permissions(survey_id,user_id,permission) VALUES(?,?,?)",
        surveyId, userId, p));
    return new AccessResponse(userId, user.username(), maskEmail(user.email()), wanted.stream().sorted().toList());
  }

  public void remove(UUID surveyId, UUID userId) {
    guard.manageAccess(surveyId);
    for (String permission : groupPermissions.direct(surveyId, userId))
      if (!guard.can(surveyId, permission)) throw new AccessDeniedException("Khong duoc thu hoi quyen vuot quyen hien co");
    jdbc.update("DELETE FROM survey_access_permissions WHERE survey_id=? AND user_id=?", surveyId, userId);
  }

  public void changeOwner(UUID surveyId, OwnerRequest request) {
    if (!guard.has("survey_update_all"))
      throw new AccessDeniedException("Chi quan tri toan he thong duoc doi tac gia survey");
    Survey survey = guard.survey(surveyId);
    UUID userId = request == null ? null : request.userId();
    UserIdentity user = activeUser(userId);
    UUID previousOwnerId = survey.getCreatorUserId();
    if (userId.equals(previousOwnerId)) return;
    survey.setCreatorUserId(userId);
    survey.setCreatorUser(user.username());
    surveys.save(survey);
    if (guard.has("survey_update_all")) alerts.ownerChangedByGlobalUser(survey, guard.userId(), previousOwnerId, userId);
  }

  private UserIdentity activeUser(UUID userId) {
    if (userId == null) throw new BadRequestException("userId khong duoc de trong");
    return jdbc.query("SELECT username,email FROM users WHERE id=? AND is_active IS TRUE",
        (rs, rowNum) -> new UserIdentity(rs.getString(1), rs.getString(2)), userId)
        .stream().findFirst().orElseThrow(() -> new NotFoundException("User dang hoat dong khong ton tai"));
  }

  private String maskEmail(String email) {
    if (email == null) return null;
    int at = email.indexOf('@');
    if (at <= 0 || at == email.length()-1) return null;
    return email.substring(0, Math.min(4, at-1)) + "****" + email.substring(at);
  }
}
