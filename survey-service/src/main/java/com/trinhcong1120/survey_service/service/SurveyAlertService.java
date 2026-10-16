package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.dto.NotificationEvent;
import com.trinhcong1120.survey_service.entity.Survey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SurveyAlertService {
  private record UserInfo(String username, String email, Long telegramChatId) {}
  private static final DateTimeFormatter VIETNAM_TIME = DateTimeFormatter
      .ofPattern("dd/MM/yyyy HH:mm:ss 'GMT+7'")
      .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, Object> kafka;

  public SurveyAlertService(JdbcTemplate jdbc, KafkaTemplate<String, Object> kafka) {
    this.jdbc = jdbc;
    this.kafka = kafka;
  }

  public void deletedByGlobalUser(Survey survey, UUID actorId) {
    UserInfo actor = user(actorId);
    UserInfo owner = user(survey.getCreatorUserId());
    String message = "Khảo sát " + survey.getTitle() + " (" + survey.getId()
        + ") đã bị xóa bởi " + display(actor, actorId)
        + ".\nChủ sở hữu trước khi xóa: " + display(owner, survey.getCreatorUserId())
        + ".\nThời điểm: " + VIETNAM_TIME.format(Instant.now()) + ".";
    publishAfterCommit("SURVEY_DELETED_BY_ADMIN", "Cảnh báo: xóa khảo sát của người khác",
        message, survey.getId(), owner);
  }

  public void ownerChangedByGlobalUser(Survey survey, UUID actorId, UUID oldOwnerId,
      UUID newOwnerId) {
    UserInfo actor = user(actorId);
    UserInfo oldOwner = user(oldOwnerId);
    UserInfo newOwner = user(newOwnerId);
    String message = "Chủ sở hữu khảo sát " + survey.getTitle() + " (" + survey.getId()
        + ") đã được đổi bởi " + display(actor, actorId)
        + ".\nChủ cũ: " + display(oldOwner, oldOwnerId)
        + ".\nChủ mới: " + display(newOwner, newOwnerId)
        + ".\nThời điểm: " + VIETNAM_TIME.format(Instant.now()) + ".";
    publishAfterCommit("SURVEY_OWNER_CHANGED_BY_ADMIN", "Cảnh báo: chuyển chủ khảo sát",
        message, survey.getId(), oldOwner, newOwner);
  }

  private void publishAfterCommit(String type, String subject, String message, UUID surveyId,
      UserInfo... owners) {
    List<UserInfo> recipients = new ArrayList<>(jdbc.query(
        "SELECT DISTINCT u.username, u.email, u.telegram_chat_id FROM users u "
            + "JOIN user_roles ur ON ur.user_id = u.id "
            + "JOIN roles r ON r.id = ur.role_id WHERE u.is_active IS TRUE "
            + "AND lower(r.name) = 'admin'",
        (rs, rowNum) -> new UserInfo(rs.getString("username"), rs.getString("email"),
            rs.getObject("telegram_chat_id", Long.class))));
    for (UserInfo owner : owners) {
      if (owner != null) recipients.add(owner);
    }
    LinkedHashMap<String, String> uniqueEmails = new LinkedHashMap<>();
    LinkedHashSet<Long> uniqueChatIds = new LinkedHashSet<>();
    for (UserInfo recipient : recipients) {
      String email = recipient.email();
      if (email != null && !email.isBlank()) {
        String trimmed = email.trim();
        uniqueEmails.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
      }
      if (recipient.telegramChatId() != null) {
        uniqueChatIds.add(recipient.telegramChatId());
      }
    }
    List<NotificationEvent> events = new ArrayList<>();
    uniqueEmails.values().forEach(email -> events.add(
        new NotificationEvent(type, email, null, subject, message, surveyId)));
    uniqueChatIds.forEach(chatId -> events.add(
        new NotificationEvent(type, null, null, subject, message, surveyId, chatId)));
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        events.forEach(event -> kafka.send("notification.events", event));
      }
    });
  }

  private UserInfo user(UUID id) {
    if (id == null) return null;
    return jdbc.query("SELECT username, email, telegram_chat_id FROM users "
            + "WHERE id = ? AND is_active IS TRUE",
        (rs, rowNum) -> new UserInfo(rs.getString("username"), rs.getString("email"),
            rs.getObject("telegram_chat_id", Long.class)), id)
        .stream().findFirst().orElse(null);
  }

  private String display(UserInfo info, UUID id) {
    return (info == null ? "tài khoản" : info.username()) + " (" + id + ")";
  }
}
