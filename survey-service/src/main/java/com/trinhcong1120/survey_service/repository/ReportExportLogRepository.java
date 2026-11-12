package com.trinhcong1120.survey_service.repository;

import com.trinhcong1120.survey_service.entity.ReportExportLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportExportLogRepository extends JpaRepository<ReportExportLog, UUID> {
  List<ReportExportLog> findTop50BySurveyIdOrderByExportedAtDesc(UUID surveyId);
}

