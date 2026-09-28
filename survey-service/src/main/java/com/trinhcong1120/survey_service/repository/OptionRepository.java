package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Integer> {

  List<Option> findByQuestion_IdOrderByOrderIndexAsc(Integer questionId);

  boolean existsByIdAndQuestion_Id(
          Integer optionId,
          Integer questionId
  );

  void deleteByQuestion_Id(Integer questionId);

  @Modifying
  @Query(
          value = """
                  DELETE FROM options
                  WHERE question_id IN (
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
