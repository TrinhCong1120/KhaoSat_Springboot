package com.trinhcong1120.core_service.dto.function;

import java.util.UUID;

public class PermissionStatusResponse {

    private UUID permissionID;
    private String code;
    private String type;
    private String action;
    private Boolean isActive;

    public PermissionStatusResponse() {
    }

    public PermissionStatusResponse(
            UUID permissionID,
            String code,
            String type,
            String action,
            Boolean isActive) {

        this.permissionID = permissionID;
        this.code = code;
        this.type = type;
        this.action = action;
        this.isActive = isActive;
    }

    public UUID getPermissionID() {
        return permissionID;
    }

    public String getCode() { return code; }
    public String getType() { return type; }

    public String getAction() {
        return action;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
