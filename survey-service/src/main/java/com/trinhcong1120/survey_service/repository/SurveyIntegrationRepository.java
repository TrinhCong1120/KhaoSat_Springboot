package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.SurveyIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SurveyIntegrationRepository extends JpaRepository<SurveyIntegration, UUID> {
    Optional<SurveyIntegration> findBySurveyId(UUID surveyId);
    Optional<SurveyIntegration> findByGoogleFormId(String googleFormId);
    List<SurveyIntegration> findByGoogleFormIdIn(java.util.Collection<String> googleFormIds);
    List<SurveyIntegration> findAllByProvider(String provider);
}
