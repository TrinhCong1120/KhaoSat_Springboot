package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {

  List<Page> findBySurvey_IdOrderByOrderIndexAsc(UUID surveyId);

  boolean existsByIdAndSurvey_Id(UUID id, UUID surveyId);

  long countBySurvey_Id(UUID surveyId);

  @Modifying
  @Query(
          value = "DELETE FROM pages WHERE survey_id = :surveyId",
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") UUID surveyId);
}
