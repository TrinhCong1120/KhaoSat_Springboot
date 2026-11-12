package com.trinhcong1120.survey_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.survey_service.dto.failed.FailedSurveyResponse;
import com.trinhcong1120.survey_service.dto.failed.ImportFailedSurveyResponse;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.exception.NotFoundException;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import io.minio.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FailedSurveyCaseService {
  public record CaseView(UUID id, String fileName, String originalFileName, long fileSize, String checksumSha256,
      String status, UUID assignedUserId, String assignedUsername, UUID createdByUserId,
      Instant createdAt, Instant updatedAt, boolean hasFile) {}
  public record EventView(UUID id, UUID actorUserId, String actorUsername, String action,
      String note, String details, Instant createdAt) {}
  public record CaseDetail(CaseView item, List<EventView> events) {}
  public record CasePage(List<CaseView> items, int page, int pageSize, long total, Map<String, Long> counts) {}
  public record Assignee(UUID id, String username) {}
  public record UpdateRequest(String status, UUID assignedUserId, boolean clearAssignee, String note) {}

  private static final Set<String> STATUSES = Set.of("NEW", "IN_PROGRESS", "RESOLVED");
  private final JdbcTemplate jdbc;
  private final MinioClient minio;
  private final ObjectMapper json;
  private final FailedSurveyService importer;
  private final SurveyAccessGuard guard;
  private final String bucket;

  public FailedSurveyCaseService(JdbcTemplate jdbc, MinioClient minio, ObjectMapper json,
      FailedSurveyService importer, SurveyAccessGuard guard,
      @Value("${app.minio.failed-bucket:failed-surveys-private}") String bucket,
      @Value("${app.minio.bucket:survey-media}") String mediaBucket) {
    if (bucket.equals(mediaBucket)) throw new IllegalArgumentException("Failed case bucket must be private");
    this.jdbc = jdbc;
    this.minio = minio;
    this.json = json;
    this.importer = importer;
    this.guard = guard;
    this.bucket = bucket;
  }

  public CasePage list(int page, int pageSize, String status, String search) {
    if (page < 1 || pageSize < 1 || pageSize > 100) throw new BadRequestException("Invalid page");
    String state = status == null || status.isBlank() ? null : status.toUpperCase();
    if (state != null && !STATUSES.contains(state)) throw new BadRequestException("Invalid status");
    String query = search == null ? "" : search.trim();
    if (query.length() > 200) throw new BadRequestException("Search is too long");
    String where = " WHERE c.status <> 'DELETED' AND (?::text IS NULL OR c.status=?::text) AND (?::text='' OR c.original_file_name ILIKE '%' || ? || '%')";
    long total = jdbc.queryForObject("SELECT count(*) FROM failed_survey_cases c" + where,
        Long.class, state, state, query, query);
    List<CaseView> items = jdbc.query("SELECT c.*, u.username AS assigned_username FROM failed_survey_cases c " +
        "LEFT JOIN users u ON u.id=c.assigned_user_id" + where +
        " ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?", this::mapCase,
        state, state, query, query, pageSize, (page - 1) * pageSize);
    Map<String, Long> counts = new java.util.HashMap<>();
    for (String value : STATUSES) counts.put(value, 0L);
    jdbc.query("SELECT status,count(*) AS count FROM failed_survey_cases WHERE status<>'DELETED' GROUP BY status",
        rs -> { counts.put(rs.getString("status"), rs.getLong("count")); });
    return new CasePage(items, page, pageSize, total, counts);
  }

  public List<Assignee> assignees() {
    return jdbc.query("SELECT DISTINCT u.id,u.username FROM users u JOIN user_roles ur ON ur.user_id=u.id " +
        "JOIN role_permissions rp ON rp.role_id=ur.role_id JOIN permissions p ON p.id=rp.permission_id " +
        "WHERE u.is_active=true AND p.code='survey_failed_manage' AND p.is_active=true ORDER BY u.username",
        (rs, n) -> new Assignee((UUID) rs.getObject("id"), rs.getString("username")));
  }

  public CaseDetail detail(UUID id) {
    CaseView item = find(id);
    List<EventView> events = jdbc.query("SELECT e.*,u.username AS actor_username FROM failed_survey_case_events e " +
        "LEFT JOIN users u ON u.id=e.actor_user_id WHERE e.case_id=? ORDER BY e.created_at,e.id",
        (rs, n) -> new EventView((UUID) rs.getObject("id"), (UUID) rs.getObject("actor_user_id"),
            rs.getString("actor_username"), rs.getString("action"), rs.getString("note"),
            rs.getString("details"), rs.getTimestamp("created_at").toInstant()), id);
    return new CaseDetail(item, events);
  }

  @Transactional
  public CaseView upload(MultipartFile file) {
    if (file == null || file.isEmpty()) throw new BadRequestException("File is empty");
    String original = file.getOriginalFilename();
    if (original == null || !original.toLowerCase().endsWith(".json"))
      throw new BadRequestException("Only JSON files are accepted");
    String safe = original.replace('\\', '/');
    safe = safe.substring(safe.lastIndexOf('/') + 1);
    if (safe.isBlank() || safe.length() > 200 || safe.contains("\r") || safe.contains("\n"))
      throw new BadRequestException("Invalid filename");
    try {
      byte[] bytes = file.getBytes();
      if (!json.readTree(bytes).isArray()) throw new BadRequestException("JSON must be an array");
      String checksum = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      UUID id = UUID.randomUUID();
      String fileName = jdbc.queryForObject("SELECT count(*) FROM failed_survey_cases WHERE file_name=?", Integer.class, safe) == 0
          ? safe : id + "-" + safe;
      String key = "failed-surveys/" + id + ".json";
      ensureBucket();
      minio.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
          .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
          .contentType("application/json").build());
      try {
        jdbc.update("INSERT INTO failed_survey_cases(id,file_name,original_file_name,object_key,file_size,checksum_sha256,created_by_user_id) VALUES (?,?,?,?,?,?,?)",
            id, fileName, safe, key, bytes.length, checksum, guard.userId());
        event(id, "CREATED", null, "{}");
      } catch (RuntimeException ex) {
        minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        throw ex;
      }
      return find(id);
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("Cannot store JSON file");
    }
  }

  @Transactional
  public CaseDetail update(UUID id, UpdateRequest request) {
    CaseView current = lock(id);
    if (current.status().equals("DELETED")) throw new BadRequestException("File was deleted");
    if (request == null) throw new BadRequestException("Update is empty");
    if (request.clearAssignee() && request.assignedUserId() != null)
      throw new BadRequestException("Conflicting assignee");
    UUID assigned = request.clearAssignee() ? null : request.assignedUserId() != null
        ? request.assignedUserId() : current.assignedUserId();
    if (request.assignedUserId() != null && !canAssign(assigned))
      throw new BadRequestException("Assignee lacks survey_failed_manage");
    String status = request.status() == null ? current.status() : request.status().toUpperCase();
    if (!STATUSES.contains(status)) throw new BadRequestException("Invalid status");
    boolean transition = !status.equals(current.status());
    if (transition && !(current.status().equals("NEW") && status.equals("IN_PROGRESS"))
        && !(current.status().equals("IN_PROGRESS") && status.equals("RESOLVED"))
        && !(current.status().equals("RESOLVED") && status.equals("IN_PROGRESS")))
      throw new BadRequestException("Invalid status transition");
    String note = request.note() == null ? null : request.note().trim();
    if (note != null && note.length() > 2000) throw new BadRequestException("Note is too long");
    boolean reassigned = !java.util.Objects.equals(assigned, current.assignedUserId());
    if (!transition && !reassigned && (note == null || note.isBlank()))
      throw new BadRequestException("Nothing to update");
    jdbc.update("UPDATE failed_survey_cases SET status=?,assigned_user_id=?,updated_at=now() WHERE id=?",
        status, assigned, id);
    if (transition) event(id, "STATUS_CHANGED", note, "{\"from\":\"" + current.status() + "\",\"to\":\"" + status + "\"}");
    if (reassigned) event(id, "ASSIGNED", note, "{\"userId\":" + (assigned == null ? "null" : "\"" + assigned + "\"") + "}");
    if (!transition && !reassigned) event(id, "NOTE", note, "{}");
    return detail(id);
  }

  @Transactional
  public ImportFailedSurveyResponse importCase(UUID id) {
    CaseView current = lock(id);
    if (current.status().equals("DELETED")) throw new BadRequestException("File was deleted");
    if (current.status().equals("RESOLVED")) throw new BadRequestException("Case already resolved");
    ImportFailedSurveyResponse result = importer.importBytes(current.originalFileName(), download(id));
    String status = result.getFailedRemain() == 0 ? "RESOLVED" : "IN_PROGRESS";
    jdbc.update("UPDATE failed_survey_cases SET status=?,updated_at=now() WHERE id=?", status, id);
    event(id, "IMPORTED", null, String.format("{\"total\":%d,\"imported\":%d,\"skippedDuplicate\":%d,\"failedRemain\":%d}",
        result.getTotal(), result.getImported(), result.getSkippedDuplicate(), result.getFailedRemain()));
    return result;
  }

  public byte[] download(UUID id) {
    String key = jdbc.query("SELECT object_key FROM failed_survey_cases WHERE id=? AND status<>'DELETED'",
        rs -> rs.next() ? rs.getString(1) : null, id);
    if (key == null) throw new NotFoundException("File not found");
    try (InputStream input = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
      return input.readAllBytes();
    } catch (Exception ex) {
      throw new BadRequestException("Cannot read stored file");
    }
  }

  @Transactional
  public void delete(UUID id) {
    CaseView current = lock(id);
    if (current.status().equals("DELETED")) throw new NotFoundException("File not found");
    String key = jdbc.queryForObject("SELECT object_key FROM failed_survey_cases WHERE id=?", String.class, id);
    try {
      minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (Exception ex) {
      throw new BadRequestException("Cannot delete stored file");
    }
    jdbc.update("UPDATE failed_survey_cases SET status='DELETED',object_key=NULL,updated_at=now() WHERE id=?", id);
    event(id, "DELETED", null, "{}");
  }

  public UUID idByFileName(String fileName) {
    List<UUID> ids = jdbc.queryForList("SELECT id FROM failed_survey_cases WHERE file_name=? AND status<>'DELETED'",
        UUID.class, fileName);
    if (ids.isEmpty()) throw new NotFoundException("File not found");
    return ids.get(0);
  }

  public List<FailedSurveyResponse> legacyList(boolean includeContent) {
    return jdbc.query("SELECT id,file_name,file_size,updated_at FROM failed_survey_cases WHERE status<>'DELETED' ORDER BY file_name",
        (rs, n) -> new FailedSurveyResponse(rs.getString("file_name"), rs.getLong("file_size"),
            LocalDateTime.ofInstant(rs.getTimestamp("updated_at").toInstant(), ZoneId.systemDefault()),
            includeContent ? new String(download((UUID) rs.getObject("id")), StandardCharsets.UTF_8) : null));
  }

  private boolean canAssign(UUID userId) {
    return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id=u.id " +
        "JOIN role_permissions rp ON rp.role_id=ur.role_id JOIN permissions p ON p.id=rp.permission_id " +
        "WHERE u.id=? AND u.is_active=true AND p.code='survey_failed_manage' AND p.is_active=true)", Boolean.class, userId));
  }

  private CaseView find(UUID id) {
    List<CaseView> rows = jdbc.query("SELECT c.*,u.username AS assigned_username FROM failed_survey_cases c " +
        "LEFT JOIN users u ON u.id=c.assigned_user_id WHERE c.id=?", this::mapCase, id);
    if (rows.isEmpty()) throw new NotFoundException("Case not found");
    return rows.get(0);
  }

  private CaseView lock(UUID id) {
    List<CaseView> rows = jdbc.query("SELECT c.*,u.username AS assigned_username FROM failed_survey_cases c " +
        "LEFT JOIN users u ON u.id=c.assigned_user_id WHERE c.id=? FOR UPDATE OF c", this::mapCase, id);
    if (rows.isEmpty()) throw new NotFoundException("Case not found");
    return rows.get(0);
  }

  private CaseView mapCase(ResultSet rs, int row) throws SQLException {
    return new CaseView((UUID) rs.getObject("id"), rs.getString("file_name"),
        rs.getString("original_file_name"), rs.getLong("file_size"), rs.getString("checksum_sha256"), rs.getString("status"),
        (UUID) rs.getObject("assigned_user_id"), rs.getString("assigned_username"),
        (UUID) rs.getObject("created_by_user_id"), rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(), rs.getString("object_key") != null);
  }

  private void event(UUID id, String action, String note, String details) {
    jdbc.update("INSERT INTO failed_survey_case_events(case_id,actor_user_id,action,note,details) VALUES (?,?,?,?,?::jsonb)",
        id, guard.userId(), action, note, details);
  }

  private void ensureBucket() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
      minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
  }
}
