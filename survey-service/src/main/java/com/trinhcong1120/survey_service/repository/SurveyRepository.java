package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, UUID> {

  Optional<Survey> findByIdAndIsActiveTrue(UUID id);

  List<Survey> findAllByOrderByCreatedAtDesc();

  @Query("""
      select s from Survey s where s.creatorUserId = :userId
      or exists (select 1 from SurveyAccess a where a.survey = s and a.userId = :userId)
      order by s.createdAt desc
      """)
  List<Survey> findVisibleTo(@Param("userId") UUID userId);

  long countByIsActiveTrue();
}
