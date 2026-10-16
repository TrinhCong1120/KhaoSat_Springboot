package com.trinhcong1120.survey_service.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/site-visits")
public class SiteVisitController {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final JdbcTemplate jdbc;

  public SiteVisitController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public record VisitRequest(UUID visitorId) {}

  @PostMapping
  public ResponseEntity<Void> record(@RequestBody VisitRequest input) {
    if (input == null || input.visitorId() == null) return ResponseEntity.badRequest().build();
    Instant now = Instant.now();
    jdbc.update("insert into site_daily_visitors(visit_date,visitor_id,first_seen_at) "
        + "values (?,?,?) on conflict do nothing", LocalDate.ofInstant(now, ZONE),
        input.visitorId(), java.sql.Timestamp.from(now));
    return ResponseEntity.noContent().build();
  }
}
