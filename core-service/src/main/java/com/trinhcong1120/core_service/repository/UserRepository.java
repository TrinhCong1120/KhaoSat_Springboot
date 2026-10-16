package com.trinhcong1120.core_service.repository;

import java.util.UUID;

import com.trinhcong1120.core_service.entity.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByTelegramChatId(Long telegramChatId);

    boolean existsByTelegramChatIdAndIdNot(Long telegramChatId, UUID id);

    Optional<User> findByIdAndIsActiveTrue(UUID id);

    @Query("select distinct u from User u join u.roles r "
            + "where u.isActive = true and lower(r.name) = 'admin'")
    List<User> findActiveAdmins();

    @Query("select u from User u where u.isActive = true "
            + "and (lower(u.username) = :query or lower(u.email) = :query) "
            + "order by lower(u.username), u.id")
    List<User> searchActiveByExactUsernameOrEmail(@Param("query") String query, Pageable pageable);
}
