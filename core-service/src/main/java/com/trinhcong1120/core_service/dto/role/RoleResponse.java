package com.trinhcong1120.core_service.dto.role;

import java.util.List;

public class RoleResponse {

    private Integer id;
    private String name;
    private List<String> permissions;

    public RoleResponse() {
    }

    public RoleResponse(
            Integer id,
            String name,
            List<String> permissions) {

        this.id = id;
        this.name = name;
        this.permissions = permissions;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}