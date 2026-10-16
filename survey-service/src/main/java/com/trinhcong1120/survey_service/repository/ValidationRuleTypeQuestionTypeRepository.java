package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.ValidationRuleTypeQuestionType;
import com.trinhcong1120.survey_service.entity.ValidationRuleTypeQuestionTypeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationRuleTypeQuestionTypeRepository
        extends JpaRepository<ValidationRuleTypeQuestionType, ValidationRuleTypeQuestionTypeId> {

  @Query("""
          select distinct mapping.id.ruleCode
          from ValidationRuleTypeQuestionType mapping
          join mapping.ruleType ruleType
          where ruleType.isActive = true
          order by mapping.id.ruleCode
          """)
  List<String> findActiveRuleCodes();

  @Query("""
          select mapping.id.ruleCode
          from ValidationRuleTypeQuestionType mapping
          join mapping.ruleType ruleType
          where ruleType.isActive = true
            and mapping.id.questionTypeCode = :questionTypeCode
          order by mapping.id.ruleCode
          """)
  List<String> findActiveRuleCodesByQuestionTypeCode(
          @Param("questionTypeCode") String questionTypeCode
  );

  @Query("""
          select mapping.id.questionTypeCode
          from ValidationRuleTypeQuestionType mapping
          where mapping.id.ruleCode = :ruleCode
          order by mapping.id.questionTypeCode
          """)
  List<String> findQuestionTypeCodesByRuleCode(@Param("ruleCode") String ruleCode);

  @Query("""
          select case when count(mapping) > 0 then true else false end
          from ValidationRuleTypeQuestionType mapping
          join mapping.ruleType ruleType
          where ruleType.isActive = true
            and mapping.id.ruleCode = :ruleCode
            and mapping.id.questionTypeCode = :questionTypeCode
          """)
  boolean existsActiveByRuleCodeAndQuestionTypeCode(
          @Param("ruleCode") String ruleCode,
          @Param("questionTypeCode") String questionTypeCode
  );
}
