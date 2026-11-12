package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.GoogleQuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleQuestionMappingRepository extends JpaRepository<GoogleQuestionMapping, UUID> {
    List<GoogleQuestionMapping> findBySurveyId(UUID surveyId);
    Optional<GoogleQuestionMapping> findBySurveyIdAndGoogleQuestionId(UUID surveyId, String googleQuestionId);
    Optional<GoogleQuestionMapping> findBySurveyIdAndInternalQuestionId(UUID surveyId, UUID internalQuestionId);
    void deleteBySurveyId(UUID surveyId);
    void deleteBySurveyIdAndInternalQuestionId(UUID surveyId, UUID internalQuestionId);
}
