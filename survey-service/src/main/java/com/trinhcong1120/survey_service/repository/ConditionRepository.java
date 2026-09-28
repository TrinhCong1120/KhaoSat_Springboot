package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionRepository
        extends JpaRepository<Condition, Integer> {

  List<Condition> findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
          Integer sourceSurveyId,
          Integer targetSurveyId
  );

  List<Condition> findBySourceQuestion_Id(Integer questionId);

  List<Condition> findByTargetQuestion_Id(Integer questionId);

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
  void deleteBySurveyId(@Param("surveyId") Integer surveyId);
}
