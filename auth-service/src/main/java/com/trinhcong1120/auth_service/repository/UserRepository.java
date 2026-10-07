package com.trinhcong1120.auth_service.repository;

import java.util.UUID;

import com.trinhcong1120.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByIdAndIsActiveTrue(UUID id);
}