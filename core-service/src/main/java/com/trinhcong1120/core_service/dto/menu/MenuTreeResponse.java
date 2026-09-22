package com.trinhcong1120.core_service.dto.menu;

import java.util.ArrayList;
import java.util.List;

public class MenuTreeResponse {

    private Integer id;
    private String name;
    private String path;
    private String icon;
    private Integer orderIndex;
    private Integer functionId;

    private List<MenuTreeResponse> children = new ArrayList<>();

    public MenuTreeResponse() {
    }

    public MenuTreeResponse(
            Integer id,
            String name,
            String path,
            String icon,
            Integer orderIndex,
            Integer functionId) {

        this.id = id;
        this.name = name;
        this.path = path;
        this.icon = icon;
        this.orderIndex = orderIndex;
        this.functionId = functionId;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public Integer getFunctionId() {
        return functionId;
    }

    public List<MenuTreeResponse> getChildren() {
        return children;
    }

    public void setChildren(List<MenuTreeResponse> children) {
        this.children = children;
    }
}