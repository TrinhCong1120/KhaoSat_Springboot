package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
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
}