package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.SurveyAccess;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAccessRepository extends JpaRepository<SurveyAccess, UUID> {
  Optional<SurveyAccess> findBySurvey_IdAndUserId(UUID surveyId, UUID userId);
  List<SurveyAccess> findBySurvey_IdOrderByUserId(UUID surveyId);
  void deleteBySurvey_Id(UUID surveyId);
  void deleteBySurvey_IdAndUserId(UUID surveyId, UUID userId);
}
