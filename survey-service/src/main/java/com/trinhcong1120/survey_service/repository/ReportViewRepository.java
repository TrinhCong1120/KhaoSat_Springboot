package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.ReportView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportViewRepository extends JpaRepository<ReportView, UUID> {

  @Query("""
      SELECT v FROM ReportView v
      WHERE v.surveyId = :surveyId
        AND (v.scope = 'SHARED' OR v.createdByUserId = :userId)
      ORDER BY v.isDefault DESC, v.updatedAt DESC
      """)
  List<ReportView> findAccessibleBySurvey(@Param("surveyId") UUID surveyId, @Param("userId") UUID userId);

  Optional<ReportView> findByIdAndSurveyId(UUID id, UUID surveyId);

  List<ReportView> findBySurveyIdAndCreatedByUserIdAndIsDefaultTrue(UUID surveyId, UUID userId);

  List<ReportView> findBySurveyIdAndScopeAndIsDefaultTrue(UUID surveyId, String scope);
}

