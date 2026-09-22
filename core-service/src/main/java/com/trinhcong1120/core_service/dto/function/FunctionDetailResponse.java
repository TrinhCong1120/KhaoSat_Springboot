package com.trinhcong1120.core_service.dto.function;

import java.util.List;

public class FunctionDetailResponse {

    private Integer functionID;
    private String functionName;

    private Integer roleID;
    private String roleName;

    private List<PermissionStatusResponse> permissions;

    public FunctionDetailResponse() {
    }

    public FunctionDetailResponse(
            Integer functionID,
            String functionName,
            Integer roleID,
            String roleName,
            List<PermissionStatusResponse> permissions) {

        this.functionID = functionID;
        this.functionName = functionName;
        this.roleID = roleID;
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public Integer getFunctionID() {
        return functionID;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Integer getRoleID() {
        return roleID;
    }

    public String getRoleName() {
        return roleName;
    }

    public List<PermissionStatusResponse> getPermissions() {
        return permissions;
    }
}