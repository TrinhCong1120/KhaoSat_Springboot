package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerOptionRepository
        extends JpaRepository<AnswerOption, UUID> {

  List<AnswerOption> findByAnswer_Id(
          UUID answerId
  );

  List<AnswerOption> findByOption_Id(
          UUID optionId
  );

  boolean existsByOption_Id(
          UUID optionId
  );

  long countByOption_Id(
          UUID optionId
  );

  @Modifying
  @Query(
          value = """
                  DELETE FROM answer_options
                  WHERE answer_id IN (
                    SELECT a.id
                    FROM answers a
                    JOIN responses r ON a.response_id = r.id
                    WHERE r.survey_id = :surveyId
                  )
                  OR option_id IN (
                    SELECT o.id
                    FROM options o
                    JOIN questions q ON o.question_id = q.id
                    JOIN pages p ON q.page_id = p.id
                    WHERE p.survey_id = :surveyId
                  )
                  """,
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") UUID surveyId);
}
