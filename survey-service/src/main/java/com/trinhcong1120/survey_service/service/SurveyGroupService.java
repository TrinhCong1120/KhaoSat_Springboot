package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SurveyGroupService {
  public record GroupRow(UUID id, String name, UUID parentId, Set<String> permissions) {}
  public record GroupRequest(String name, UUID parentId) {}
  public record AccessRequest(List<String> permissions) {}
  public record AccessRow(UUID userId, String username, List<String> permissions) {}
  public record UserRow(UUID id, String username) {}
  private final JdbcTemplate jdbc;
  private final GroupPermissions permissions;
  private final SurveyAccessGuard guard;

  public SurveyGroupService(JdbcTemplate jdbc, GroupPermissions permissions, SurveyAccessGuard guard) {
    this.jdbc = jdbc;
    this.permissions = permissions;
    this.guard = guard;
  }

  public Set<String> effective(UUID id) {
    Set<String> result = permissions.group(id, guard.userId());
    if (guard.has("survey_update_all")) result.addAll(Set.of("VIEW", "CREATE_CHILD", "EDIT",
        "MANAGE_PUBLICATION", "MANAGE_ACCESS", "MANAGE_GROUP"));
    if (guard.has("survey_view_all")) result.addAll(Set.of("VIEW", "VIEW_RESPONSES", "EXPORT_DATA"));
    if (guard.has("survey_delete_all")) result.add("DELETE");
    if (guard.has("survey_create") && guard.has("survey_update_all")) result.add("CREATE_SURVEY");
    return result;
  }

  public void require(UUID id, String permission) {
    if (!exists(id)) throw new NotFoundException("Group khong ton tai");
    if (!effective(id).contains(permission)) throw new AccessDeniedException("Khong co quyen " + permission + " trong group");
  }

  private boolean exists(UUID id) {
    return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM survey_groups WHERE id=?)", Boolean.class, id));
  }

  @Transactional(readOnly = true)
  public List<GroupRow> visible() {
    List<GroupRow> result = new ArrayList<>();
    jdbc.query("SELECT id,name,parent_id FROM survey_groups ORDER BY name,id", rs -> {
      UUID id = rs.getObject("id", UUID.class);
      Set<String> rights = effective(id);
      if (!rights.isEmpty()) result.add(new GroupRow(id, rs.getString("name"),
          rs.getObject("parent_id", UUID.class), rights));
    });
    return result;
  }

  public GroupRow create(GroupRequest request) {
    String name = name(request);
    UUID parent = request.parentId();
    if (parent == null) {
      if (!guard.has("survey_update_all")) throw new AccessDeniedException("Khong co quyen tao group goc");
    } else require(parent, "CREATE_CHILD");
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO survey_groups(id,name,parent_id) VALUES(?,?,?)", id, name, parent);
    return new GroupRow(id, name, parent, effective(id));
  }

  public GroupRow rename(UUID id, GroupRequest request) {
    require(id, "MANAGE_GROUP");
    String name = name(request);
    jdbc.update("UPDATE survey_groups SET name=? WHERE id=?", name, id);
    UUID parent = jdbc.queryForObject("SELECT parent_id FROM survey_groups WHERE id=?", UUID.class, id);
    return new GroupRow(id, name, parent, effective(id));
  }

  public void delete(UUID id) {
    require(id, "MANAGE_GROUP");
    if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM survey_groups WHERE parent_id=?)", Boolean.class, id))
        || Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM surveys WHERE group_id=?)", Boolean.class, id)))
      throw new BadRequestException("Chi duoc xoa group rong");
    jdbc.update("DELETE FROM survey_groups WHERE id=?", id);
  }

  @Transactional(readOnly = true)
  public List<AccessRow> access(UUID id) {
    require(id, "MANAGE_GROUP");
    return jdbc.query("SELECT ga.user_id,u.username,ga.permission FROM survey_group_access ga "
        + "JOIN users u ON u.id=ga.user_id WHERE ga.group_id=? ORDER BY u.username,ga.user_id,ga.permission",
        rs -> {
          List<AccessRow> rows = new ArrayList<>();
          while (rs.next()) {
            UUID userId = rs.getObject(1, UUID.class);
            AccessRow old = rows.isEmpty() ? null : rows.get(rows.size()-1);
            if (old == null || !old.userId().equals(userId)) {
              old = new AccessRow(userId, rs.getString(2), new ArrayList<>());
              rows.add(old);
            }
            old.permissions().add(rs.getString(3));
          }
          return rows;
        }, id);
  }

  @Transactional(readOnly = true)
  public List<UserRow> searchUsers(UUID id, String query) {
    require(id, "MANAGE_GROUP");
    if (query == null || query.isBlank() || query.length() > 255) throw new BadRequestException("Tu khoa khong hop le");
    return jdbc.query("SELECT id,username FROM users WHERE is_active IS TRUE "
        + "AND (lower(username)=lower(?) OR lower(email)=lower(?)) LIMIT 10",
        (rs, n) -> new UserRow(rs.getObject(1, UUID.class), rs.getString(2)), query.strip(), query.strip());
  }

  public void putAccess(UUID id, UUID userId, AccessRequest request) {
    require(id, "MANAGE_GROUP");
    Set<String> wanted = new HashSet<>(request == null || request.permissions() == null ? List.of() : request.permissions());
    if (wanted.isEmpty() || !GroupPermissions.GROUP.containsAll(wanted)) throw new BadRequestException("Quyen group khong hop le");
    Set<String> existing = new HashSet<>(jdbc.queryForList("SELECT permission FROM survey_group_access WHERE group_id=? AND user_id=?",
        String.class, id, userId));
    Set<String> changed = new HashSet<>(wanted);
    changed.removeAll(existing);
    existing.removeAll(wanted);
    changed.addAll(existing);
    if (!effective(id).containsAll(changed)) throw new AccessDeniedException("Khong duoc thay doi quyen vuot quyen hien co");
    if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM users WHERE id=? AND is_active IS TRUE)", Boolean.class, userId)))
      throw new NotFoundException("User dang hoat dong khong ton tai");
    jdbc.update("DELETE FROM survey_group_access WHERE group_id=? AND user_id=?", id, userId);
    wanted.forEach(p -> jdbc.update("INSERT INTO survey_group_access(group_id,user_id,permission) VALUES(?,?,?)", id, userId, p));
  }

  public void removeAccess(UUID id, UUID userId) {
    require(id, "MANAGE_GROUP");
    Set<String> existing = new HashSet<>(jdbc.queryForList("SELECT permission FROM survey_group_access WHERE group_id=? AND user_id=?",
        String.class, id, userId));
    if (!effective(id).containsAll(existing)) throw new AccessDeniedException("Khong duoc thu hoi quyen vuot quyen hien co");
    jdbc.update("DELETE FROM survey_group_access WHERE group_id=? AND user_id=?", id, userId);
  }

  private String name(GroupRequest request) {
    String value = request == null ? null : request.name();
    if (value == null || value.isBlank() || value.strip().length() > 255)
      throw new BadRequestException("Ten group phai co 1-255 ky tu");
    return value.strip();
  }
}
