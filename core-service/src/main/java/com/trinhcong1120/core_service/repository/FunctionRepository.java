package com.trinhcong1120.core_service.repository;

import java.util.UUID;

import com.trinhcong1120.core_service.entity.Function;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FunctionRepository
        extends JpaRepository<Function, UUID> {

    Optional<Function> findByCode(String code);
}