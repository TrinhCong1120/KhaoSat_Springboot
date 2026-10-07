package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionTypeRepository
        extends JpaRepository<QuestionType, UUID> {

  Optional<QuestionType> findByCode(String code);

  boolean existsByCode(String code);
}