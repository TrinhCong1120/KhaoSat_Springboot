package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

class SurveyGroupServiceTest {
  private final UUID userId = UUID.randomUUID();
  private final UUID parent = UUID.randomUUID();
  private final UUID child = UUID.randomUUID();
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final GroupPermissions permissions = mock(GroupPermissions.class);
  private final SurveyAccessGuard guard = mock(SurveyAccessGuard.class);
  private final SurveyGroupService service = new SurveyGroupService(jdbc, permissions, guard);

  @Test void directChildRightsDoNotRequireParentRights() {
    when(guard.userId()).thenReturn(userId);
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(UUID.class))).thenReturn(true);
    when(permissions.group(parent, userId)).thenReturn(Set.of());
    when(permissions.group(child, userId)).thenReturn(Set.of("CREATE_CHILD", "MANAGE_GROUP"));
    assertThrows(AccessDeniedException.class, () -> service.require(parent, "CREATE_CHILD"));
    assertDoesNotThrow(() -> service.require(child, "CREATE_CHILD"));
    assertDoesNotThrow(() -> service.require(child, "MANAGE_GROUP"));
  }

  @Test void managerCannotGrantRightTheyDoNotHold() {
    when(guard.userId()).thenReturn(userId);
    when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(UUID.class))).thenReturn(true);
    when(jdbc.queryForList(anyString(), eq(String.class), any(UUID.class), any(UUID.class))).thenReturn(List.of());
    when(permissions.group(child, userId)).thenReturn(Set.of("MANAGE_GROUP", "VIEW"));
    UUID otherUser = UUID.randomUUID();
    assertThrows(AccessDeniedException.class,
        () -> service.putAccess(child, otherUser, new SurveyGroupService.AccessRequest(List.of("DELETE"))));
    verify(jdbc, never()).update(anyString(), any(Object[].class));
  }
}
