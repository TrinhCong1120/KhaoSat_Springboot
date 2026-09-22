package com.trinhcong1120.core_service.dto.function;

import jakarta.validation.constraints.NotNull;

public class UpdatePermissionRequest {

    @NotNull(message = "ID vai trò không được để trống")
    private Integer roleId;

    @NotNull(message = "ID quyền không được để trống")
    private Integer permissionId;

    @NotNull(message = "Trạng thái quyền không được để trống")
    private Boolean isActive;

    public UpdatePermissionRequest() {
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
