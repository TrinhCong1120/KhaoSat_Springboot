package com.trinhcong1120.survey_service.dto.report;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ReportViewDto(
    UUID id,
    UUID surveyId,
    String name,
    String description,
    String scope,
    JsonNode definition,
    UUID createdByUserId,
    boolean isDefault,
    boolean canEdit,
    Instant createdAt,
    Instant updatedAt
) {}

