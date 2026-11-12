package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.service.SurveyAccessService;
import com.trinhcong1120.survey_service.service.SurveyAlertService;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;

class SurveyAccessResponseTest {
  private final SurveyAccessGuard guard = mock(SurveyAccessGuard.class);
  private final SurveyRepository surveys = mock(SurveyRepository.class);
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final GroupPermissions permissions = mock(GroupPermissions.class);
  private final SurveyAccessService service = new SurveyAccessService(guard, surveys, jdbc, mock(SurveyAlertService.class), permissions);

  @Test @SuppressWarnings("unchecked")
  void putReturnsMaskedEmailAndIndependentRights() throws Exception {
    UUID surveyId = UUID.randomUUID(), userId = UUID.randomUUID();
    when(guard.can(surveyId, "VIEW_RESPONSES")).thenReturn(true);
    when(guard.can(surveyId, "MANAGE_ACCESS")).thenReturn(true);
    when(permissions.direct(surveyId, userId)).thenReturn(Set.of());
    ResultSet row = mock(ResultSet.class);
    when(row.getString(1)).thenReturn("alice");
    when(row.getString(2)).thenReturn("a@gmail.com");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(userId))).thenAnswer(call ->
        List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));

    var result = service.put(surveyId, userId,
        new SurveyAccessService.AccessRequest(List.of("VIEW_RESPONSES", "MANAGE_ACCESS")));

    assertEquals("alice", result.username());
    assertEquals("****@gmail.com", result.email());
    assertEquals(List.of("MANAGE_ACCESS", "VIEW_RESPONSES"), result.permissions());
  }

  @Test @SuppressWarnings("unchecked")
  void cannotGrantRightActorDoesNotHave() throws Exception {
    UUID surveyId = UUID.randomUUID(), userId = UUID.randomUUID();
    when(permissions.direct(surveyId, userId)).thenReturn(Set.of());
    ResultSet row = mock(ResultSet.class);
    when(row.getString(1)).thenReturn("alice");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(userId))).thenAnswer(call ->
        List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));
    assertThrows(AccessDeniedException.class,
        () -> service.put(surveyId, userId, new SurveyAccessService.AccessRequest(List.of("DELETE"))));
    verify(jdbc, never()).update(anyString(), any(Object[].class));
  }

  @Test @SuppressWarnings("unchecked")
  void changingCreatorDoesNotGrantOrRemoveSurveyRights() throws Exception {
    UUID surveyId = UUID.randomUUID(), previous = UUID.randomUUID(), next = UUID.randomUUID();
    Survey survey = new Survey(); survey.setCreatorUserId(previous);
    when(guard.survey(surveyId)).thenReturn(survey);
    when(guard.has("survey_update_all")).thenReturn(true);
    ResultSet row = mock(ResultSet.class);
    when(row.getString(1)).thenReturn("next");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(next))).thenAnswer(call ->
        List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));
    service.changeOwner(surveyId, new SurveyAccessService.OwnerRequest(next));
    assertEquals(next, survey.getCreatorUserId());
    verify(surveys).save(survey);
    verify(jdbc, never()).update(anyString(), any(Object[].class));
  }

  @Test void managerPermissionIsRequiredToChangeCreator() {
    UUID surveyId = UUID.randomUUID();
    assertThrows(AccessDeniedException.class,
        () -> service.changeOwner(surveyId, new SurveyAccessService.OwnerRequest(UUID.randomUUID())));
    verify(surveys, never()).save(any());
  }
}
