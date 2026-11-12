package com.trinhcong1120.survey_service.service;

import com.trinhcong1120.survey_service.client.CoreUserClient;
import com.trinhcong1120.survey_service.dto.core.CoreUserResponse;
import com.trinhcong1120.survey_service.entity.GoogleConnection;
import com.trinhcong1120.survey_service.entity.GoogleFormAccessGrant;
import com.trinhcong1120.survey_service.entity.SurveyIntegration;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.GoogleConnectionRepository;
import com.trinhcong1120.survey_service.repository.GoogleFormAccessGrantRepository;
import com.trinhcong1120.survey_service.repository.SurveyIntegrationRepository;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GoogleFormAccessService {

    private static final Logger log = LoggerFactory.getLogger(GoogleFormAccessService.class);

    private final SurveyAccessGuard accessGuard;
    private final SurveyIntegrationRepository integrationRepo;
    private final GoogleConnectionRepository connectionRepo;
    private final GoogleFormAccessGrantRepository grantRepo;
    private final GoogleDriveService driveService;
    private final CoreUserClient coreUserClient;

    public GoogleFormAccessService(
            SurveyAccessGuard accessGuard,
            SurveyIntegrationRepository integrationRepo,
            GoogleConnectionRepository connectionRepo,
            GoogleFormAccessGrantRepository grantRepo,
            GoogleDriveService driveService,
            CoreUserClient coreUserClient
    ) {
        this.accessGuard = accessGuard;
        this.integrationRepo = integrationRepo;
        this.connectionRepo = connectionRepo;
        this.grantRepo = grantRepo;
        this.driveService = driveService;
        this.coreUserClient = coreUserClient;
    }

    public record EditAccessResponse(String editUrl, String recipientEmail, String status, String message) {}

    public EditAccessResponse requestEditAccess(UUID surveyId, String authHeader) {
        // 1. Check internal EDIT permission
        accessGuard.require(surveyId, "EDIT");
        UUID currentUserId = accessGuard.userId();

        // 2. Fetch survey integration
        SurveyIntegration integration = integrationRepo.findBySurveyId(surveyId)
                .orElseThrow(() -> new BadRequestException("Khảo sát này không liên kết với Google Forms."));

        // 3. Determine recipient Google email ONLY from linked active Google connection
        String recipientEmail = null;
        List<GoogleConnection> userConnections = connectionRepo.findByUserId(currentUserId);
        for (GoogleConnection c : userConnections) {
            if ("ACTIVE".equals(c.getStatus()) && c.getGoogleEmail() != null && !c.getGoogleEmail().isBlank()) {
                recipientEmail = c.getGoogleEmail();
                break;
            }
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new BadRequestException("NO_GOOGLE_CONNECTION: Bạn chưa liên kết tài khoản Google nào. Vui lòng liên kết tài khoản Google để được cấp quyền chỉnh sửa biểu mẫu.");
        }

        // 4. Determine owner connection
        UUID ownerConnId = integration.getOwnerConnectionId();
        GoogleConnection ownerConnection = null;
        if (ownerConnId != null) {
            ownerConnection = connectionRepo.findById(ownerConnId).orElse(null);
        }

        // If owner connection not found or inactive, fallback to any active connection of the survey creator
        if (ownerConnection == null || !"ACTIVE".equals(ownerConnection.getStatus())) {
            var survey = accessGuard.survey(surveyId);
            if (survey.getCreatorUserId() != null) {
                List<GoogleConnection> creatorConns = connectionRepo.findByUserId(survey.getCreatorUserId());
                ownerConnection = creatorConns.stream()
                        .filter(c -> "ACTIVE".equals(c.getStatus()))
                        .findFirst()
                        .orElse(null);
                if (ownerConnection != null && integration.getOwnerConnectionId() == null) {
                    integration.setOwnerConnectionId(ownerConnection.getId());
                    integrationRepo.save(integration);
                }
            }
        }

        if (ownerConnection == null) {
            throw new BadRequestException("Người sở hữu Form chưa kết nối tài khoản Google hoặc kết nối đã hết hạn. Vui lòng liên hệ chủ sở hữu để kết nối lại.");
        }

        // 5. If recipient is already the owner of the form, no need to grant again
        if (ownerConnection.getGoogleEmail() != null && ownerConnection.getGoogleEmail().equalsIgnoreCase(recipientEmail)) {
            return new EditAccessResponse(
                    integration.getEditUrl(),
                    recipientEmail,
                    "OWNER",
                    "Bạn là chủ sở hữu của biểu mẫu Google này."
            );
        }

        // 6. If already granted writer permission, return directly
        Optional<GoogleFormAccessGrant> existingGrant = grantRepo.findBySurveyIntegrationIdAndGoogleEmail(integration.getId(), recipientEmail);
        if (existingGrant.isPresent() && "GRANTED".equals(existingGrant.get().getStatus())) {
            return new EditAccessResponse(
                    integration.getEditUrl(),
                    recipientEmail,
                    "ALREADY_GRANTED",
                    "Bạn đã có quyền chỉnh sửa biểu mẫu này."
            );
        }

        // 7. Grant writer permission via Google Drive API
        String permissionId = null;
        try {
            permissionId = driveService.grantWriterPermission(ownerConnection, integration.getGoogleFormId(), recipientEmail);

            GoogleFormAccessGrant grant = existingGrant.orElseGet(GoogleFormAccessGrant::new);
            grant.setSurveyIntegrationId(integration.getId());
            grant.setUserId(currentUserId);
            grant.setGoogleEmail(recipientEmail);
            grant.setGooglePermissionId(permissionId);
            grant.setRole("writer");
            grant.setStatus("GRANTED");
            grant.setGrantedByUserId(ownerConnection.getUserId());
            grant.setGrantedAt(Instant.now());
            grant.setLastError(null);
            grantRepo.save(grant);

            return new EditAccessResponse(
                    integration.getEditUrl(),
                    recipientEmail,
                    "GRANTED",
                    "Đã cấp quyền chỉnh sửa cho " + recipientEmail
            );
        } catch (Exception e) {
            log.error("Failed to grant writer permission: {}", e.getMessage(), e);

            GoogleFormAccessGrant grant = existingGrant.orElseGet(GoogleFormAccessGrant::new);
            grant.setSurveyIntegrationId(integration.getId());
            grant.setUserId(currentUserId);
            grant.setGoogleEmail(recipientEmail);
            grant.setRole("writer");
            grant.setStatus("FAILED");
            grant.setGrantedByUserId(ownerConnection.getUserId());
            grant.setLastError(e.getMessage());
            grantRepo.save(grant);

            return new EditAccessResponse(
                    integration.getEditUrl(),
                    recipientEmail,
                    "FALLBACK",
                    "Mở trực tiếp biểu mẫu Google Forms."
            );
        }
    }
}
