package com.trinhcong1120.core_service.dto.menu;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public class UpdateMenuRequest {

    @NotBlank(message = "Tên menu không được để trống")
    private String name;

    private String path;
    private UUID parentId;
    private String icon;
    private Integer orderIndex;
    private UUID functionId;

    public UpdateMenuRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public UUID getFunctionId() {
        return functionId;
    }

    public void setFunctionId(UUID functionId) {
        this.functionId = functionId;
    }
}
