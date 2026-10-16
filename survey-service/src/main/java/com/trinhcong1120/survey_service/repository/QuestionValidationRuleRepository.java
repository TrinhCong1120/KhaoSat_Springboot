package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.QuestionValidationRule;
import com.trinhcong1120.survey_service.entity.QuestionValidationRuleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionValidationRuleRepository
        extends JpaRepository<QuestionValidationRule, QuestionValidationRuleId> {

  List<QuestionValidationRule> findByQuestion_Id(UUID questionId);

  List<QuestionValidationRule> findByQuestion_Page_Survey_IdAndIsActiveTrue(UUID surveyId);

  void deleteByQuestion_Id(UUID questionId);
}
