package com.trinhcong1120.core_service.dto.role;

import jakarta.validation.constraints.NotBlank;

public class UpdateRoleRequest {

    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;

    public UpdateRoleRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
