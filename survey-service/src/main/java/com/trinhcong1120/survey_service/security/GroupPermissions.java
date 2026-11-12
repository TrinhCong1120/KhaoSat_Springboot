package com.trinhcong1120.survey_service.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupPermissions {
  public static final Set<String> SURVEY = Set.of("VIEW", "EDIT", "VIEW_RESPONSES",
      "EXPORT_DATA", "MANAGE_PUBLICATION", "DELETE", "MANAGE_ACCESS");
  public static final Set<String> GROUP = Set.of("VIEW", "CREATE_CHILD", "CREATE_SURVEY", "EDIT",
      "VIEW_RESPONSES", "EXPORT_DATA", "MANAGE_PUBLICATION", "DELETE", "MANAGE_ACCESS", "MANAGE_GROUP");
  private final JdbcTemplate jdbc;

  public GroupPermissions(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Set<String> group(UUID groupId, UUID userId) {
    if (groupId == null) return new HashSet<>();
    return new HashSet<>(jdbc.queryForList("""
        WITH RECURSIVE ancestors AS (
          SELECT id,parent_id FROM survey_groups WHERE id=?
          UNION ALL SELECT g.id,g.parent_id FROM survey_groups g JOIN ancestors a ON g.id=a.parent_id
        ) SELECT DISTINCT a.permission FROM survey_group_access a
          JOIN ancestors g ON g.id=a.group_id WHERE a.user_id=?
        """, String.class, groupId, userId));
  }

  public Set<String> survey(UUID surveyId, UUID groupId, UUID userId) {
    Set<String> result = group(groupId, userId);
    result.addAll(direct(surveyId, userId));
    return result;
  }

  public Set<String> direct(UUID surveyId, UUID userId) {
    return new HashSet<>(jdbc.queryForList("SELECT permission FROM survey_access_permissions WHERE survey_id=? AND user_id=?",
        String.class, surveyId, userId));
  }

  public List<UUID> directlySharedSurveyIds(UUID userId) {
    return jdbc.queryForList("SELECT DISTINCT survey_id FROM survey_access_permissions WHERE user_id=?",
        UUID.class, userId);
  }

  public boolean hasCurrentGlobalPermission(UUID userId, String code) {
    return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM user_roles ur "
        + "JOIN role_permissions rp ON rp.role_id=ur.role_id "
        + "JOIN permissions p ON p.id=rp.permission_id "
        + "WHERE ur.user_id=? AND p.code=? AND p.is_active IS TRUE)", Boolean.class, userId, code));
  }

  public List<UUID> visibleSurveyIds(UUID userId, boolean responsesOnly) {
    String predicate = responsesOnly
        ? "p.permission = 'VIEW_RESPONSES'"
        : "p.permission IN ('VIEW','EDIT','VIEW_RESPONSES','EXPORT_DATA','MANAGE_PUBLICATION','DELETE','MANAGE_ACCESS')";
    String sql = """
        WITH RECURSIVE granted_groups AS (
          SELECT ga.group_id AS id,ga.permission FROM survey_group_access ga WHERE ga.user_id=?
          UNION ALL SELECT g.id,gg.permission FROM survey_groups g
            JOIN granted_groups gg ON g.parent_id=gg.id
        ) SELECT DISTINCT s.id FROM surveys s WHERE EXISTS (
          SELECT 1 FROM granted_groups gg WHERE gg.id=s.group_id AND %s
        ) OR EXISTS (SELECT 1 FROM survey_access_permissions p WHERE p.survey_id=s.id
          AND p.user_id=? AND %s)
        """.formatted(responsesOnly
            ? "gg.permission = 'VIEW_RESPONSES'"
            : "gg.permission IN ('VIEW','EDIT','VIEW_RESPONSES','EXPORT_DATA','MANAGE_PUBLICATION','DELETE','MANAGE_ACCESS')",
            predicate);
    return jdbc.queryForList(sql, UUID.class, userId, userId);
  }
}
