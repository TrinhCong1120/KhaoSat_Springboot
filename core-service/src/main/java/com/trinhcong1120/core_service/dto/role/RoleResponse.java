package com.trinhcong1120.core_service.dto.role;

import java.util.UUID;

import java.util.List;

public class RoleResponse {

    private UUID id;
    private String name;
    private List<String> permissions;

    public RoleResponse() {
    }

    public RoleResponse(
            UUID id,
            String name,
            List<String> permissions) {

        this.id = id;
        this.name = name;
        this.permissions = permissions;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}