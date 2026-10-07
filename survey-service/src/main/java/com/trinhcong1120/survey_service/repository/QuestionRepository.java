package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

  List<Question> findByPage_IdOrderByOrderIndexAsc(UUID pageId);

  List<Question> findByPage_Survey_Id(UUID surveyId);

  long countByPage_Survey_Id(UUID surveyId);

  long countByPage_Survey_IdAndIsRequiredTrue(UUID surveyId);

  @Modifying
  @Query(
          value = """
                  DELETE FROM questions
                  WHERE page_id IN (
                    SELECT id
                    FROM pages
                    WHERE survey_id = :surveyId
                  )
                  """,
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") UUID surveyId);
}
