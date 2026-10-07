package com.trinhcong1120.auth_service.dto;

import java.util.UUID;

public class RegisterResponse {

    private UUID id;
    private String username;
    private Boolean isActive;

    public RegisterResponse(
            UUID id,
            String username,
            Boolean isActive) {

        this.id = id;
        this.username = username;
        this.isActive = isActive;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
