package com.trinhcong1120.core_service.dto.user;

import java.util.UUID;
import java.util.List;

public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private Boolean isActive;
    private List<String> roles;
    private Long telegramChatId;
    private String zaloChatId;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private Boolean botNotificationEnabled;

    public UserResponse() {
    }

    public UserResponse(
            UUID id,
            String username,
            String email,
            Boolean isActive,
            List<String> roles,
            Long telegramChatId,
            String zaloChatId) {
        this(id, username, email, isActive, roles, telegramChatId, zaloChatId, null, null, null, true);
    }

    public UserResponse(
            UUID id,
            String username,
            String email,
            Boolean isActive,
            List<String> roles,
            Long telegramChatId,
            String zaloChatId,
            String fullName,
            String phoneNumber,
            String avatarUrl,
            Boolean botNotificationEnabled) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.isActive = isActive;
        this.roles = roles;
        this.telegramChatId = telegramChatId;
        this.zaloChatId = zaloChatId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.botNotificationEnabled = botNotificationEnabled != null ? botNotificationEnabled : true;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public String getZaloChatId() {
        return zaloChatId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Boolean getBotNotificationEnabled() {
        return botNotificationEnabled;
    }
}
