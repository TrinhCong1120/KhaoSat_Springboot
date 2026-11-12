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

  @Query("""
      select c from Condition c
      join c.sourceQuestion sq
      join sq.page sp
      left join c.targetQuestion tq
      left join tq.page tqp
      left join c.targetPage tp
      where sp.survey.id = :surveyId
      or (tqp is not null and tqp.survey.id = :surveyId)
      or (tp is not null and tp.survey.id = :surveyId)
  """)
  List<Condition> findBySourceQuestion_Page_Survey_IdOrTargetQuestion_Page_Survey_Id(
          @Param("surveyId") UUID sourceSurveyId,
          @Param("surveyId") UUID targetSurveyId
  );

  @Query("""
      select c from Condition c
      join c.sourceQuestion sq
      join sq.page sp
      left join c.targetQuestion tq
      left join tq.page tqp
      left join c.targetPage tp
      where sp.survey.id = :surveyId
      or (tqp is not null and tqp.survey.id = :surveyId)
      or (tp is not null and tp.survey.id = :surveyId)
  """)
  List<Condition> findBySurveyId(@Param("surveyId") UUID surveyId);

  List<Condition> findBySourceQuestion_Id(UUID questionId);

  List<Condition> findByTargetQuestion_Id(UUID questionId);

  List<Condition> findByTargetPage_Id(UUID pageId);

  @Query("""
      select c from Condition c
      join c.sourceQuestion sq
      join sq.page sp
      left join c.targetQuestion tq
      left join tq.page tqp
      left join c.targetPage tp
      where sp.survey.id in :surveyIds
      and (
        (c.targetType = 'QUESTION' and tqp is not null and tqp.survey.id = sp.survey.id)
        or (c.targetType = 'PAGE' and tp is not null and tp.survey.id = sp.survey.id)
        or (tq is not null and tqp.survey.id = sp.survey.id)
        or (tp is not null and tp.survey.id = sp.survey.id)
      )
  """)
  List<Condition> findBySurveyIds(@Param("surveyIds") List<UUID> surveyIds);

  @Query("""
      select c from Condition c
      join c.sourceQuestion sq
      join sq.page sp
      left join c.targetQuestion tq
      left join tq.page tqp
      left join c.targetPage tp
      where sp.survey.id = :surveyId
      and (
        (c.targetType = 'QUESTION' and tqp is not null and tqp.survey.id = :surveyId)
        or (c.targetType = 'PAGE' and tp is not null and tp.survey.id = :surveyId)
        or (tq is not null and tqp.survey.id = :surveyId)
        or (tp is not null and tp.survey.id = :surveyId)
      )
  """)
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
                  OR target_page_id IN (
                    SELECT p.id
                    FROM pages p
                    WHERE p.survey_id = :surveyId
                  )
                  """,
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") UUID surveyId);
}
