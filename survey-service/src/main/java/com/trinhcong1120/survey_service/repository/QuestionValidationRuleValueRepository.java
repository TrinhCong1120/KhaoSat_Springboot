package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.QuestionValidationRuleValue;
import com.trinhcong1120.survey_service.entity.QuestionValidationRuleValueId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionValidationRuleValueRepository
        extends JpaRepository<QuestionValidationRuleValue, QuestionValidationRuleValueId> {

  List<QuestionValidationRuleValue> findById_QuestionId(UUID questionId);
}
