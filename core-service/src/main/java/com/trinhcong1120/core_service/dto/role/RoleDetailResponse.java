package com.trinhcong1120.core_service.dto.role;

import java.util.UUID;

import java.util.List;

public class RoleDetailResponse {

    private UUID id;
    private String name;
    private List<PermissionInfo> permissions;

    public RoleDetailResponse() {
    }

    public RoleDetailResponse(
            UUID id,
            String name,
            List<PermissionInfo> permissions) {

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

    public List<PermissionInfo> getPermissions() {
        return permissions;
    }

    public static class PermissionInfo {

        private UUID id;
        private String code;
        private String name;
        private String type;

        public PermissionInfo() {
        }

        public PermissionInfo(
                UUID id,
                String code,
                String name,
                String type) {

            this.id = id;
            this.code = code;
            this.name = name;
            this.type = type;
        }

        public UUID getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
        public String getType() { return type; }
    }
}
