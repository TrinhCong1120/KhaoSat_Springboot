package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.entity.SurveyIntegration;
import com.trinhcong1120.survey_service.repository.SurveyIntegrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(GoogleSyncScheduler.class);

    private final SurveyIntegrationRepository integrationRepo;
    private final GoogleResponseSyncService syncService;

    public GoogleSyncScheduler(SurveyIntegrationRepository integrationRepo, GoogleResponseSyncService syncService) {
        this.integrationRepo = integrationRepo;
        this.syncService = syncService;
    }

    // Run every 5 minutes (300,000 ms), starting 1 minute after application boot
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void syncAllGoogleForms() {
        List<SurveyIntegration> integrations = integrationRepo.findAllByProvider("GOOGLE_FORMS");
        if (integrations.isEmpty()) {
            return;
        }

        log.info("Starting scheduled background sync for {} Google Form integrations", integrations.size());
        for (SurveyIntegration integration : integrations) {
            try {
                syncService.syncResponses(integration.getSurveyId(), false);
            } catch (Exception e) {
                log.warn("Scheduled sync failed for survey {}: {}", integration.getSurveyId(), e.getMessage());
            }
        }
    }
}
