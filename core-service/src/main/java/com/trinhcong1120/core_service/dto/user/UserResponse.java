package com.trinhcong1120.core_service.dto.user;

import java.util.List;

public class UserResponse {

    private Integer id;
    private String username;
    private String email;
    private Boolean isActive;
    private List<String> roles;

    public UserResponse() {
    }

    public UserResponse(
            Integer id,
            String username,
            String email,
            Boolean isActive,
            List<String> roles) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.isActive = isActive;
        this.roles = roles;
    }

    public Integer getId() {
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
}
