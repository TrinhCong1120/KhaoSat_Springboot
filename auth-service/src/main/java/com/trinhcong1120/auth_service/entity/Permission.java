package com.trinhcong1120.auth_service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "is_active")
    private Boolean isActive;

    public Permission() {
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Boolean getIsActive() {
        return isActive;
    }
}