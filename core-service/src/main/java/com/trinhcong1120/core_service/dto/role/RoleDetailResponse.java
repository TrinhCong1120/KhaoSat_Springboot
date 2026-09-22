package com.trinhcong1120.core_service.dto.role;

import java.util.List;

public class RoleDetailResponse {

    private Integer id;
    private String name;
    private List<PermissionInfo> permissions;

    public RoleDetailResponse() {
    }

    public RoleDetailResponse(
            Integer id,
            String name,
            List<PermissionInfo> permissions) {

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

    public List<PermissionInfo> getPermissions() {
        return permissions;
    }

    public static class PermissionInfo {

        private Integer id;
        private String code;
        private String name;

        public PermissionInfo() {
        }

        public PermissionInfo(
                Integer id,
                String code,
                String name) {

            this.id = id;
            this.code = code;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }
}