package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository
        extends JpaRepository<Answer, Integer> {

  List<Answer> findByResponse_Id(
          Integer responseId
  );

  List<Answer> findByQuestion_Id(
          Integer questionId
  );

  Optional<Answer> findByResponse_IdAndQuestion_Id(
          Integer responseId,
          Integer questionId
  );

  boolean existsByQuestion_Id(
          Integer questionId
  );

  long countByQuestion_Id(
          Integer questionId
  );

  @Modifying
  @Query(
          value = """
                  DELETE FROM answers
                  WHERE response_id IN (
                    SELECT id
                    FROM responses
                    WHERE survey_id = :surveyId
                  )
                  OR question_id IN (
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
