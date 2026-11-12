package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.GoogleConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleConnectionRepository extends JpaRepository<GoogleConnection, UUID> {
    List<GoogleConnection> findByUserId(UUID userId);
    Optional<GoogleConnection> findByUserIdAndGoogleEmail(UUID userId, String googleEmail);
    Optional<GoogleConnection> findByUserIdAndGoogleSubject(UUID userId, String googleSubject);
    Optional<GoogleConnection> findByIdAndUserId(UUID id, UUID userId);
}
