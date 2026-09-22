package com.trinhcong1120.core_service.dto.event;

public class NotificationEvent {

    private String eventType;
    private String email;
    private String username;
    private String subject;
    private String message;
    private Integer referenceId;

    public NotificationEvent() {
    }

    public NotificationEvent(
            String eventType,
            String email,
            String username,
            String subject,
            String message,
            Integer referenceId
    ) {
        this.eventType = eventType;
        this.email = email;
        this.username = username;
        this.subject = subject;
        this.message = message;
        this.referenceId = referenceId;
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

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }
}