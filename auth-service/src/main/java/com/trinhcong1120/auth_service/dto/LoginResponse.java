package com.trinhcong1120.auth_service.dto;

import java.util.List;

public class LoginResponse {

    private String token;
    private Integer id;
    private String username;
    private List<String> roles;
    private List<String> permissions;

    public LoginResponse(
            String token,
            Integer id,
            String username,
            List<String> roles,
            List<String> permissions) {

        this.token = token;
        this.id = id;
        this.username = username;
        this.roles = roles;
        this.permissions = permissions;
    }

    public String getToken() {
        return token;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}