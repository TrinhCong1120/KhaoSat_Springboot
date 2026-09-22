package com.trinhcong1120.core_service.dto.role;

import jakarta.validation.constraints.NotBlank;

public class CreateRoleRequest {

    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;

    public CreateRoleRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
