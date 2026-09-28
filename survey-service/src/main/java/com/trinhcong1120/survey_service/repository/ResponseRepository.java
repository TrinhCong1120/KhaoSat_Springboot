package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResponseRepository
        extends JpaRepository<Response, Integer> {

  List<Response> findBySurvey_IdOrderBySubmittedAtDesc(
          Integer surveyId
  );

  Optional<Response> findByRequestId(UUID requestId);

  boolean existsByRequestId(UUID requestId);

  long countBySurvey_Id(Integer surveyId);

  long countBySurvey_IdAndSubmittedAtBetween(
          Integer surveyId,
          LocalDateTime from,
          LocalDateTime to
  );

  long countBySubmittedAtBetween(
          LocalDateTime from,
          LocalDateTime to
  );

  @Modifying
  @Query(
          value = "DELETE FROM responses WHERE survey_id = :surveyId",
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") Integer surveyId);
}
