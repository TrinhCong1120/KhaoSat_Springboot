package com.trinhcong1120.survey_service.repository;

import java.util.UUID;

import com.trinhcong1120.survey_service.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, UUID> {

  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("select s from Survey s where s.id = :id")
  Optional<Survey> findForRead(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Survey s where s.id = :id")
  Optional<Survey> findForWrite(@Param("id") UUID id);

  List<Survey> findAllByOrderByCreatedAtDesc();
  List<Survey> findByPublishedAtIsNotNull();

  Optional<Survey> findByShareCode(String shareCode);

  boolean existsByShareCode(String shareCode);

  @Query("""
      select s from Survey s where s.creatorUserId = :userId
      or exists (select 1 from SurveyAccess a where a.survey = s and a.userId = :userId and a.canView = true)
      order by s.createdAt desc
      """)
  List<Survey> findVisibleTo(@Param("userId") UUID userId);

  @Query("""
      select s from Survey s where s.creatorUserId = :userId
      or exists (select 1 from SurveyAccess a where a.survey = s and a.userId = :userId and a.canView = true)
      """)
  Page<Survey> findVisibleTo(@Param("userId") UUID userId, Pageable pageable);

}
