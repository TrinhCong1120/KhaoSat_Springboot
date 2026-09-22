package com.trinhcong1120.auth_service.dto;

public class RegisterResponse {

    private Integer id;
    private String username;
    private Boolean isActive;

    public RegisterResponse(
            Integer id,
            String username,
            Boolean isActive) {

        this.id = id;
        this.username = username;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
