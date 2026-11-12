package com.trinhcong1120.survey_service.dto.survey;

import java.time.Instant;
import java.util.UUID;

public record GoogleDriveFormItem(
        String id,
        String name,
        String editUrl,
        String responderUri,
        Instant modifiedTime,
        boolean alreadyLinked,
        UUID linkedSurveyId
) {}
