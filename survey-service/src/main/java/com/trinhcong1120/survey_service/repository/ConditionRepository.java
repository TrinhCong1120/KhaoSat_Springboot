package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionRepository
        extends JpaRepository<Condition, UUID> {

  List<Condition> findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
          UUID sourceSurveyId,
          UUID targetSurveyId
  );

  List<Condition> findBySourceQuestion_Id(UUID questionId);

  List<Condition> findByTargetQuestion_Id(UUID questionId);

  @Query("select c from Condition c where c.sourceQuestion.page.survey.id in :surveyIds " +
      "and c.sourceQuestion.page.survey.id = c.targetQuestion.page.survey.id")
  List<Condition> findBySurveyIds(@Param("surveyIds") List<UUID> surveyIds);

  @Query("select c from Condition c where c.sourceQuestion.page.survey.id = :surveyId " +
      "and c.targetQuestion.page.survey.id = :surveyId")
  List<Condition> findSafeBySurveyId(@Param("surveyId") UUID surveyId);

  @Modifying
  @Query(
          value = """
                  DELETE FROM conditions
                  WHERE source_question_id IN (
                    SELECT q.id
                    FROM questions q
                    JOIN pages p ON q.page_id = p.id
                    WHERE p.survey_id = :surveyId
                  )
                  OR target_question_id IN (
                    SELECT q.id
                    FROM questions q
                    JOIN pages p ON q.page_id = p.id
                    WHERE p.survey_id = :surveyId
                  )
                  """,
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") UUID surveyId);
}
