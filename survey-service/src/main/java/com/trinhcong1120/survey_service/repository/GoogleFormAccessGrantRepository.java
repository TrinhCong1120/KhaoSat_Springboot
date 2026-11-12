package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.GoogleFormAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleFormAccessGrantRepository extends JpaRepository<GoogleFormAccessGrant, UUID> {
    Optional<GoogleFormAccessGrant> findBySurveyIntegrationIdAndUserId(UUID surveyIntegrationId, UUID userId);
    Optional<GoogleFormAccessGrant> findBySurveyIntegrationIdAndGoogleEmail(UUID surveyIntegrationId, String googleEmail);
}
