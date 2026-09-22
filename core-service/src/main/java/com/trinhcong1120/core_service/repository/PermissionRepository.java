package com.trinhcong1120.core_service.repository;

import com.trinhcong1120.core_service.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository
        extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByCode(String code);
}