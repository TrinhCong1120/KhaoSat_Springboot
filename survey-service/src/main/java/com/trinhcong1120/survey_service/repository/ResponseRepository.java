package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResponseRepository
        extends JpaRepository<Response, UUID>, JpaSpecificationExecutor<Response> {

  List<Response> findBySurvey_IdOrderBySubmittedAtDesc(
          UUID surveyId
  );

  List<Response> findBySurvey_IdIn(List<UUID> surveyIds);

  Optional<Response> findByRequestId(UUID requestId);

  boolean existsByRequestId(UUID requestId);

  long countBySurvey_Id(UUID surveyId);

  long countBySurvey_IdAndSubmittedAtBetween(
          UUID surveyId,
          Instant from,
          Instant to
  );

  long countBySubmittedAtBetween(
          Instant from,
          Instant to
  );

  @Modifying
  @Query(
          value = "DELETE FROM responses WHERE survey_id = :surveyId",
          nativeQuery = true
  )
  void deleteBySurveyId(@Param("surveyId") UUID surveyId);
}
