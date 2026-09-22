package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
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
}