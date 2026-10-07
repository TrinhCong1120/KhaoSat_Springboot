package com.trinhcong1120.core_service.dto.menu;

import java.util.UUID;

import java.time.LocalDateTime;

public class MenuResponse {

    private UUID id;
    private String name;
    private String path;
    private UUID parentId;
    private String icon;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private UUID functionId;

    public MenuResponse() {
    }

    public MenuResponse(
            UUID id,
            String name,
            String path,
            UUID parentId,
            String icon,
            Integer orderIndex,
            LocalDateTime createdAt,
            UUID functionId
    ) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.parentId = parentId;
        this.icon = icon;
        this.orderIndex = orderIndex;
        this.createdAt = createdAt;
        this.functionId = functionId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getFunctionId() {
        return functionId;
    }

    public void setFunctionId(UUID functionId) {
        this.functionId = functionId;
    }
}