package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerOptionRepository
        extends JpaRepository<AnswerOption, Integer> {

  List<AnswerOption> findByAnswer_Id(
          Integer answerId
  );

  List<AnswerOption> findByOption_Id(
          Integer optionId
  );

  boolean existsByOption_Id(
          Integer optionId
  );

  long countByOption_Id(
          Integer optionId
  );
}