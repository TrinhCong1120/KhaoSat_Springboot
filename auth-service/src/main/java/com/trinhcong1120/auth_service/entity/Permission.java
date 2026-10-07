package com.trinhcong1120.auth_service.entity;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "is_active")
    private Boolean isActive;

    public Permission() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getType() { return type; }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}
