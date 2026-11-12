package com.trinhcong1120.survey_service.dto;

import java.util.UUID;

public record NotificationEvent(String eventType, String email, String username,
                                String subject, String message, UUID referenceId,
                                Long telegramChatId, String zaloChatId,
                                UUID actorId, UUID recipientId, UUID eventId) {
  public NotificationEvent(String eventType, String email, String username,
      String subject, String message, UUID referenceId) {
    this(eventType, email, username, subject, message, referenceId, null, null, null, null, UUID.randomUUID());
  }
  public NotificationEvent(String eventType, String email, String username,
      String subject, String message, UUID referenceId, Long telegramChatId) {
    this(eventType, email, username, subject, message, referenceId, telegramChatId, null, null, null, UUID.randomUUID());
  }
  public NotificationEvent(String eventType, String email, String username,
      String subject, String message, UUID referenceId, Long telegramChatId,
      UUID actorId, UUID recipientId) {
    this(eventType, email, username, subject, message, referenceId, telegramChatId, null,
        actorId, recipientId, UUID.randomUUID());
  }
  public static NotificationEvent zalo(String eventType, String subject, String message,
      UUID referenceId, String zaloChatId, UUID actorId) {
    return new NotificationEvent(eventType, null, null, subject, message, referenceId,
        null, zaloChatId, actorId, null, UUID.randomUUID());
  }
}
