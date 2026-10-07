package com.trinhcong1120.core_service.dto.function;

import java.util.UUID;

import java.util.List;

public class FunctionDetailResponse {

    private UUID functionID;
    private String functionName;

    private UUID roleID;
    private String roleName;

    private List<PermissionStatusResponse> permissions;

    public FunctionDetailResponse() {
    }

    public FunctionDetailResponse(
            UUID functionID,
            String functionName,
            UUID roleID,
            String roleName,
            List<PermissionStatusResponse> permissions) {

        this.functionID = functionID;
        this.functionName = functionName;
        this.roleID = roleID;
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public UUID getFunctionID() {
        return functionID;
    }

    public String getFunctionName() {
        return functionName;
    }

    public UUID getRoleID() {
        return roleID;
    }

    public String getRoleName() {
        return roleName;
    }

    public List<PermissionStatusResponse> getPermissions() {
        return permissions;
    }
}