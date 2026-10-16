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

    public UserResponse() {
    }

    public UserResponse(
            UUID id,
            String username,
            String email,
            Boolean isActive,
            List<String> roles,
            Long telegramChatId) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.isActive = isActive;
        this.roles = roles;
        this.telegramChatId = telegramChatId;
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
}
