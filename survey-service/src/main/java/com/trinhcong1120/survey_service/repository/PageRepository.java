package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

  List<Page> findBySurvey_IdOrderByOrderIndexAsc(Integer surveyId);

  long countBySurvey_Id(Integer surveyId);

  @Modifying
  @Query(
          value = "DELETE FROM pages WHERE survey_id = :surveyId",
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") Integer surveyId);
}
