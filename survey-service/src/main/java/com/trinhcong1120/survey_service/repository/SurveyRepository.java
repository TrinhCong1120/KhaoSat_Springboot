package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Integer> {

  Optional<Survey> findByIdAndIsActiveTrue(Integer id);

  List<Survey> findAllByOrderByCreatedAtDesc();

  long countByIsActiveTrue();
}