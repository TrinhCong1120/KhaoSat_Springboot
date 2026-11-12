package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.ExternalSurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalSurveyResponseRepository extends JpaRepository<ExternalSurveyResponse, UUID> {
    Optional<ExternalSurveyResponse> findByGoogleFormIdAndGoogleResponseId(String googleFormId, String googleResponseId);
    List<ExternalSurveyResponse> findBySurveyId(UUID surveyId);
    long countBySurveyId(UUID surveyId);
}
