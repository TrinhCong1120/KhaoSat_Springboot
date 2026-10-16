package com.trinhcong1120.survey_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.survey_service.dto.NotificationEvent;
import com.trinhcong1120.survey_service.entity.Survey;
import com.trinhcong1120.survey_service.repository.SurveyAccessRepository;
import com.trinhcong1120.survey_service.repository.SurveyRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SurveyCriticalAlertTest {
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  @SuppressWarnings("unchecked")
  private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
  private final SurveyAlertService alerts = new SurveyAlertService(jdbc, kafka);

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletedSurveyNotifiesAdminsAndPreviousOwnerAfterCommit() throws Exception {
    UUID ownerId = UUID.randomUUID(), actorId = UUID.randomUUID();
    Survey survey = new Survey(); survey.setId(UUID.randomUUID()); survey.setTitle("Annual survey");
    survey.setCreatorUserId(ownerId);
    when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(call -> {
      ResultSet rs = mock(ResultSet.class);
      when(rs.getString("username")).thenReturn("admin");
      when(rs.getString("email")).thenReturn("admin@example.com");
      when(rs.getObject("telegram_chat_id", Long.class)).thenReturn(100L);
      RowMapper<Object> mapper = call.getArgument(1);
      return List.of(mapper.mapRow(rs, 0));
    });
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class))).thenAnswer(call -> {
      UUID id = call.getArgument(2);
      ResultSet rs = mock(ResultSet.class);
      when(rs.getString("username")).thenReturn(id.equals(actorId) ? "operator" : "owner");
      when(rs.getString("email")).thenReturn(id.equals(actorId) ? "operator@example.com" : "owner@example.com");
      when(rs.getObject("telegram_chat_id", Long.class)).thenReturn(id.equals(actorId) ? 900L : 200L);
      RowMapper<Object> mapper = call.getArgument(1);
      return List.of(mapper.mapRow(rs, 0));
    });

    TransactionSynchronizationManager.initSynchronization();
    alerts.deletedByGlobalUser(survey, actorId);
    verifyNoInteractions(kafka);
    TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

    var event = org.mockito.ArgumentCaptor.forClass(Object.class);
    verify(kafka, times(4)).send(eq("notification.events"), event.capture());
    List<NotificationEvent> sent = event.getAllValues().stream().map(NotificationEvent.class::cast).toList();
    assertEquals(List.of("admin@example.com", "owner@example.com"),
        sent.stream().map(NotificationEvent::email).filter(java.util.Objects::nonNull).toList());
    assertEquals(List.of(100L, 200L), sent.stream().map(NotificationEvent::telegramChatId)
        .filter(java.util.Objects::nonNull).toList());
    assertEquals("SURVEY_DELETED_BY_ADMIN", sent.get(0).eventType());
    assertTrue(sent.get(0).message().contains("operator"));
    assertTrue(sent.get(0).message().contains("owner"));
    assertTrue(sent.get(0).message().contains("Annual survey"));
    assertTrue(sent.get(0).message().contains("GMT+7"));
  }

  @Test
  void ownerTransferByGlobalUserTriggersAlertButOwnerTransferDoesNot() {
    SurveyAccessGuard guard = mock(SurveyAccessGuard.class);
    SurveyRepository surveys = mock(SurveyRepository.class);
    SurveyAccessRepository accesses = mock(SurveyAccessRepository.class);
    SurveyAlertService notifier = mock(SurveyAlertService.class);
    SurveyAccessService service = new SurveyAccessService(guard, surveys, accesses, jdbc, notifier);
    UUID surveyId = UUID.randomUUID(), ownerId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID(), newOwnerId = UUID.randomUUID();
    Survey survey = new Survey(); survey.setId(surveyId); survey.setCreatorUserId(ownerId);
    when(guard.survey(surveyId)).thenReturn(survey);
    when(guard.userId()).thenReturn(actorId);
    when(guard.has("survey_update_all")).thenReturn(true);
    when(jdbc.query(anyString(), any(RowMapper.class), eq(newOwnerId))).thenAnswer(call -> {
      ResultSet rs = mock(ResultSet.class);
      when(rs.getString("username")).thenReturn("new-owner");
      when(rs.getString("email")).thenReturn("new@example.com");
      RowMapper<Object> mapper = call.getArgument(1);
      return List.of(mapper.mapRow(rs, 0));
    });

    service.changeOwner(surveyId, new SurveyAccessService.OwnerRequest(newOwnerId));
    verify(notifier).ownerChangedByGlobalUser(survey, actorId, ownerId, newOwnerId);
    assertEquals(newOwnerId, survey.getCreatorUserId());

    reset(notifier);
    when(guard.userId()).thenReturn(newOwnerId);
    service.changeOwner(surveyId, new SurveyAccessService.OwnerRequest(newOwnerId));
    verifyNoInteractions(notifier);
  }

  @Test
  @SuppressWarnings("unchecked")
  void transferEmailIncludesBothOwnersAndAdmins() throws Exception {
    UUID oldOwnerId = UUID.randomUUID(), newOwnerId = UUID.randomUUID(), actorId = UUID.randomUUID();
    Survey survey = new Survey(); survey.setId(UUID.randomUUID()); survey.setTitle("Transferred survey");
    when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(call -> {
      ResultSet rs = mock(ResultSet.class);
      when(rs.getString("username")).thenReturn("admin");
      when(rs.getString("email")).thenReturn("admin@example.com");
      when(rs.getObject("telegram_chat_id", Long.class)).thenReturn(100L);
      RowMapper<Object> mapper = call.getArgument(1);
      return List.of(mapper.mapRow(rs, 0));
    });
    when(jdbc.query(anyString(), any(RowMapper.class), any(UUID.class))).thenAnswer(call -> {
      UUID id = call.getArgument(2);
      ResultSet rs = mock(ResultSet.class);
      String name = id.equals(oldOwnerId) ? "old-owner" : id.equals(newOwnerId) ? "new-owner" : "operator";
      when(rs.getString("username")).thenReturn(name);
      when(rs.getString("email")).thenReturn(name + "@example.com");
      when(rs.getObject("telegram_chat_id", Long.class)).thenReturn(
          id.equals(oldOwnerId) ? 200L : id.equals(newOwnerId) ? 300L : 900L);
      RowMapper<Object> mapper = call.getArgument(1);
      return List.of(mapper.mapRow(rs, 0));
    });

    TransactionSynchronizationManager.initSynchronization();
    alerts.ownerChangedByGlobalUser(survey, actorId, oldOwnerId, newOwnerId);
    verifyNoInteractions(kafka);
    TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

    var event = org.mockito.ArgumentCaptor.forClass(Object.class);
    verify(kafka, times(6)).send(eq("notification.events"), event.capture());
    List<NotificationEvent> sent = event.getAllValues().stream().map(NotificationEvent.class::cast).toList();
    assertEquals(List.of("admin@example.com", "old-owner@example.com", "new-owner@example.com"),
        sent.stream().map(NotificationEvent::email).filter(java.util.Objects::nonNull).toList());
    assertEquals(List.of(100L, 200L, 300L), sent.stream().map(NotificationEvent::telegramChatId)
        .filter(java.util.Objects::nonNull).toList());
    assertEquals("SURVEY_OWNER_CHANGED_BY_ADMIN", sent.get(0).eventType());
    assertTrue(sent.get(0).message().contains("old-owner"));
    assertTrue(sent.get(0).message().contains("new-owner"));
  }
}
