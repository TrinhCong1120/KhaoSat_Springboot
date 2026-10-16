package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.ValidationRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationRuleTypeRepository
        extends JpaRepository<ValidationRuleType, String> {
}
