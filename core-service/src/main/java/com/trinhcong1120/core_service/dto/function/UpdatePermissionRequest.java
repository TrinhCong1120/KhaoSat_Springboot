package com.trinhcong1120.core_service.dto.function;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public class UpdatePermissionRequest {

    @NotNull(message = "ID vai trò không được để trống")
    private UUID roleId;

    @NotNull(message = "ID quyền không được để trống")
    private UUID permissionId;

    @NotNull(message = "Trạng thái quyền không được để trống")
    private Boolean isActive;

    public UpdatePermissionRequest() {
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
