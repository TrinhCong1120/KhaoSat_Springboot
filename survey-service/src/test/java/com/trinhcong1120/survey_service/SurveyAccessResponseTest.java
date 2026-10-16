package com.trinhcong1120.survey_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.entity.SurveyAccess;
import com.trinhcong1120.survey_service.repository.SurveyAccessRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import com.trinhcong1120.survey_service.service.SurveyAccessService;
import com.trinhcong1120.survey_service.service.SurveyAccessService.AccessRequest;
import com.trinhcong1120.survey_service.service.SurveyAccessService.AccessResponse;
import com.trinhcong1120.survey_service.service.SurveyAccessService.OwnerRequest;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class SurveyAccessResponseTest {
  @Mock SurveyAccessGuard guard;
  @Mock SurveyRepository surveys;
  @Mock SurveyAccessRepository accesses;
  @Mock JdbcTemplate jdbc;
  @InjectMocks SurveyAccessService service;

  @Test
  @SuppressWarnings("unchecked")
  void listIncludesUsernameAndMaskedEmail() throws Exception {
    UUID surveyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ResultSet row = org.mockito.Mockito.mock(ResultSet.class);
    when(row.getObject("user_id", UUID.class)).thenReturn(userId);
    when(row.getString("username")).thenReturn("testuser");
    when(row.getString("email")).thenReturn("testuser@gmail.com");
    when(row.getBoolean("can_edit")).thenReturn(true);
    when(jdbc.query(anyString(), any(RowMapper.class), eq(surveyId)))
        .thenAnswer(call -> {
          RowMapper<AccessResponse> mapper = call.getArgument(1);
          return List.of(mapper.mapRow(row, 0));
        });

    AccessResponse result = service.list(surveyId).get(0);

    assertEquals(userId, result.userId());
    assertEquals("testuser", result.username());
    assertEquals("test****@gmail.com", result.email());
    assertEquals(List.of("VIEW", "EDIT"), result.permissions());
  }

  @Test
  @SuppressWarnings("unchecked")
  void putReturnsMaskedEmailToo() throws Exception {
    UUID surveyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setCreatorUserId(UUID.randomUUID());
    when(guard.survey(surveyId)).thenReturn(survey);
    ResultSet row = org.mockito.Mockito.mock(ResultSet.class);
    when(row.getString("username")).thenReturn("alice");
    when(row.getString("email")).thenReturn("a@gmail.com");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(userId)))
        .thenAnswer(call -> {
          RowMapper<?> mapper = call.getArgument(1);
          return List.of(mapper.mapRow(row, 0));
        });
    when(accesses.findBySurvey_IdAndUserId(surveyId, userId)).thenReturn(Optional.empty());
    when(accesses.save(any(SurveyAccess.class))).thenAnswer(call -> call.getArgument(0));

    AccessResponse result = service.put(surveyId, userId, new AccessRequest(List.of("VIEW")));

    assertEquals("alice", result.username());
    assertEquals("****@gmail.com", result.email());
    assertEquals(List.of("VIEW"), result.permissions());
  }

  @Test
  @SuppressWarnings("unchecked")
  void ownerCanTransferAndKeepsViewEditAccess() throws Exception {
    UUID surveyId = UUID.randomUUID();
    UUID oldOwnerId = UUID.randomUUID();
    UUID newOwnerId = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setCreatorUserId(oldOwnerId);
    when(guard.survey(surveyId)).thenReturn(survey);
    ResultSet row = org.mockito.Mockito.mock(ResultSet.class);
    when(row.getString("username")).thenReturn("new-owner");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(newOwnerId)))
        .thenAnswer(call -> List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));
    when(accesses.findBySurvey_IdAndUserId(surveyId, oldOwnerId)).thenReturn(Optional.empty());

    service.changeOwner(surveyId, new OwnerRequest(newOwnerId));

    verify(accesses).deleteBySurvey_IdAndUserId(surveyId, newOwnerId);
    ArgumentCaptor<SurveyAccess> savedAccess = ArgumentCaptor.forClass(SurveyAccess.class);
    verify(accesses).save(savedAccess.capture());
    assertEquals(oldOwnerId, savedAccess.getValue().getUserId());
    assertEquals(survey, savedAccess.getValue().getSurvey());
    assertTrue(savedAccess.getValue().isCanView());
    assertTrue(savedAccess.getValue().isCanEdit());
    assertEquals(newOwnerId, survey.getCreatorUserId());
    assertEquals("new-owner", survey.getCreatorUser());
    verify(surveys).save(survey);
  }

  @Test
  void editAccessAloneCannotTransferOwner() {
    UUID surveyId = UUID.randomUUID();
    doThrow(new AccessDeniedException("Khong co quyen quan ly access"))
        .when(guard).manageAccess(surveyId);

    assertThrows(AccessDeniedException.class,
        () -> service.changeOwner(surveyId, new OwnerRequest(UUID.randomUUID())));

    verify(surveys, never()).save(any());
    verify(accesses, never()).save(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void transferToSameOwnerDoesNotCreateOwnerAccess() throws Exception {
    UUID surveyId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setCreatorUserId(ownerId);
    when(guard.survey(surveyId)).thenReturn(survey);
    ResultSet row = org.mockito.Mockito.mock(ResultSet.class);
    when(row.getString("username")).thenReturn("owner");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(ownerId)))
        .thenAnswer(call -> List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));

    service.changeOwner(surveyId, new OwnerRequest(ownerId));

    verify(accesses, never()).save(any());
    verify(accesses, never()).deleteBySurvey_IdAndUserId(any(), any());
    verify(surveys, never()).save(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void adminCanTransferAndUpgradesExistingOldOwnerAccess() throws Exception {
    UUID surveyId = UUID.randomUUID();
    UUID oldOwnerId = UUID.randomUUID();
    UUID newOwnerId = UUID.randomUUID();
    Survey survey = new Survey();
    survey.setCreatorUserId(oldOwnerId);
    SurveyAccess existing = new SurveyAccess();
    existing.setCanView(true);
    when(guard.survey(surveyId)).thenReturn(survey);
    ResultSet row = org.mockito.Mockito.mock(ResultSet.class);
    when(row.getString("username")).thenReturn("new-owner");
    when(jdbc.query(anyString(), any(RowMapper.class), eq(newOwnerId)))
        .thenAnswer(call -> List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));
    when(accesses.findBySurvey_IdAndUserId(surveyId, oldOwnerId)).thenReturn(Optional.of(existing));

    service.changeOwner(surveyId, new OwnerRequest(newOwnerId));

    assertTrue(existing.isCanView());
    assertTrue(existing.isCanEdit());
    assertEquals(oldOwnerId, existing.getUserId());
    verify(accesses).save(existing);
    verify(surveys).save(survey);
  }
}
