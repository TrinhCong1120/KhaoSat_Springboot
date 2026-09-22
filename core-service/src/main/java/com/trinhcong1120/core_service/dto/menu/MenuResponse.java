package com.trinhcong1120.core_service.dto.menu;

import java.time.LocalDateTime;

public class MenuResponse {

    private Integer id;
    private String name;
    private String path;
    private Integer parentId;
    private String icon;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private Integer functionId;

    public MenuResponse() {
    }

    public MenuResponse(
            Integer id,
            String name,
            String path,
            Integer parentId,
            String icon,
            Integer orderIndex,
            LocalDateTime createdAt,
            Integer functionId
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
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

    public Integer getFunctionId() {
        return functionId;
    }

    public void setFunctionId(Integer functionId) {
        this.functionId = functionId;
    }
}