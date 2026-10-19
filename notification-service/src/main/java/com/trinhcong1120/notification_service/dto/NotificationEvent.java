package com.trinhcong1120.notification_service.dto;

import java.util.UUID;

public class NotificationEvent {

    private String eventType;

    private String email;

    private String username;

    private String subject;

    private String message;

    private UUID referenceId;

    private Long telegramChatId;
    private UUID actorId;
    private UUID recipientId;
    private UUID eventId;

    public NotificationEvent() {
    }

    public NotificationEvent(
            String eventType,
            String email,
            String username,
            String subject,
            String message,
            UUID referenceId
    ) {
        this(eventType, email, username, subject, message, referenceId, null);
    }

    public NotificationEvent(
            String eventType,
            String email,
            String username,
            String subject,
            String message,
            UUID referenceId,
            Long telegramChatId
    ) {
        this.eventType = eventType;
        this.email = email;
        this.username = username;
        this.subject = subject;
        this.message = message;
        this.referenceId = referenceId;
        this.telegramChatId = telegramChatId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
}
