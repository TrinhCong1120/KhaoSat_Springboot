package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.service.SurveyGroupService;
import com.trinhcong1120.survey_service.service.SurveyGroupService.AccessRequest;
import com.trinhcong1120.survey_service.service.SurveyGroupService.AccessRow;
import com.trinhcong1120.survey_service.service.SurveyGroupService.GroupRequest;
import com.trinhcong1120.survey_service.service.SurveyGroupService.GroupRow;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Groups")
public class SurveyGroupController {
  private final SurveyGroupService groups;
  public SurveyGroupController(SurveyGroupService groups) { this.groups = groups; }

  @GetMapping public List<GroupRow> list() { return groups.visible(); }
  @PostMapping public GroupRow create(@RequestBody GroupRequest request) { return groups.create(request); }
  @PutMapping("/{id}") public GroupRow rename(@PathVariable UUID id, @RequestBody GroupRequest request) {
    return groups.rename(id, request);
  }
  @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) {
    groups.delete(id); return ResponseEntity.noContent().build();
  }
  @GetMapping("/{id}/access") public List<AccessRow> access(@PathVariable UUID id) { return groups.access(id); }
  @GetMapping("/{id}/users/search") public List<SurveyGroupService.UserRow> search(@PathVariable UUID id,
      @RequestParam String q) { return groups.searchUsers(id, q); }
  @PutMapping("/{id}/access/{userId}") public ResponseEntity<Void> putAccess(@PathVariable UUID id,
      @PathVariable UUID userId, @RequestBody AccessRequest request) {
    groups.putAccess(id, userId, request); return ResponseEntity.noContent().build();
  }
  @DeleteMapping("/{id}/access/{userId}") public ResponseEntity<Void> removeAccess(@PathVariable UUID id,
      @PathVariable UUID userId) {
    groups.removeAccess(id, userId); return ResponseEntity.noContent().build();
  }
}
