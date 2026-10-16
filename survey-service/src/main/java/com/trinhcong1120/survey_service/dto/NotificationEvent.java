package com.trinhcong1120.survey_service.dto;

import java.util.UUID;

public record NotificationEvent(String eventType, String email, String username,
                                String subject, String message, UUID referenceId,
                                Long telegramChatId) {
  public NotificationEvent(String eventType, String email, String username,
      String subject, String message, UUID referenceId) {
    this(eventType, email, username, subject, message, referenceId, null);
  }
}
