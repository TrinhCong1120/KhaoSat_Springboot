package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

  List<Question> findByPage_IdOrderByOrderIndexAsc(Integer pageId);

  List<Question> findByPage_Survey_Id(Integer surveyId);

  long countByPage_Survey_Id(Integer surveyId);

  long countByPage_Survey_IdAndIsRequiredTrue(Integer surveyId);
}