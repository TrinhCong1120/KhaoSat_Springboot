package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Groups/dashboard")
public class ScopedDashboardController {
  public record Row(UUID surveyId, String title, long responses) {}
  public record Summary(long surveys, long responses, List<Row> rows) {}
  private final GroupPermissions permissions;
  private final SurveyAccessGuard guard;
  private final JdbcTemplate jdbc;

  public ScopedDashboardController(GroupPermissions permissions, SurveyAccessGuard guard, JdbcTemplate jdbc) {
    this.permissions = permissions; this.guard = guard; this.jdbc = jdbc;
  }

  @GetMapping
  public Summary get(@RequestParam(required = false) UUID groupId) {
    List<UUID> ids = guard.has("survey_view_all")
        ? jdbc.queryForList("SELECT id FROM surveys", UUID.class)
        : permissions.visibleSurveyIds(guard.userId(), true);
    return summary(ids, groupId);
  }

  private Summary summary(List<UUID> ids, UUID groupId) {
    if (ids.isEmpty()) return new Summary(0, 0, List.of());
    String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
    List<UUID> params = new ArrayList<>(ids);
    if (groupId != null) params.add(groupId);
    List<Row> rows = jdbc.query("SELECT s.id,s.title,count(r.id) FROM surveys s "
        + "LEFT JOIN responses r ON r.survey_id=s.id WHERE s.id IN (" + placeholders + ") "
        + (groupId == null ? "" : "AND s.group_id=? ")
        + "GROUP BY s.id,s.title", (rs, index) -> new Row(rs.getObject(1, UUID.class),
            rs.getString(2), rs.getLong(3)), params.toArray());
    long responses = rows.stream().mapToLong(Row::responses).sum();
    rows.sort((a,b) -> Long.compare(b.responses(), a.responses()));
    return new Summary(rows.size(), responses, rows);
  }

  @GetMapping("/surveys.xlsx")
  public ResponseEntity<byte[]> export(@RequestParam(required = false) UUID groupId) throws IOException {
    List<UUID> candidates = guard.has("survey_view_all")
        ? jdbc.queryForList("SELECT id FROM surveys", UUID.class)
        : permissions.visibleSurveyIds(guard.userId(), false);
    Summary summary = summary(candidates.stream().filter(id -> guard.can(id, "EXPORT_DATA")).toList(), groupId);
    try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Khao sat");
      var head = sheet.createRow(0);
      head.createCell(0).setCellValue("Khảo sát");
      head.createCell(1).setCellValue("Phản hồi");
      int index = 1;
      for (Row row : summary.rows()) {
        var line = sheet.createRow(index++);
        line.createCell(0).setCellValue(row.title());
        line.createCell(1).setCellValue(row.responses());
      }
      workbook.write(out);
      byte[] bytes = out.toByteArray();
      return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename=\"group-surveys.xlsx\"")
          .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(bytes);
    }
  }
}
