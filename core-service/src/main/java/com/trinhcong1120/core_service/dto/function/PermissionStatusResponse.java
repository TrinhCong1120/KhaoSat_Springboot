package com.trinhcong1120.core_service.dto.function;

public class PermissionStatusResponse {

    private Integer permissionID;
    private String action;
    private Boolean isActive;

    public PermissionStatusResponse() {
    }

    public PermissionStatusResponse(
            Integer permissionID,
            String action,
            Boolean isActive) {

        this.permissionID = permissionID;
        this.action = action;
        this.isActive = isActive;
    }

    public Integer getPermissionID() {
        return permissionID;
    }

    public String getAction() {
        return action;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}