package com.trinhcong1120.survey_service.service;

import com.google.api.services.drive.model.File;
import com.google.api.services.forms.v1.model.Form;
import com.trinhcong1120.survey_service.client.CoreUserClient;
import com.trinhcong1120.survey_service.dto.core.CoreUserResponse;
import com.trinhcong1120.survey_service.dto.survey.GoogleDriveFormItem;
import com.trinhcong1120.survey_service.dto.survey.GoogleIntegrationDto;
import com.trinhcong1120.survey_service.dto.survey.SurveyResponse;
import com.trinhcong1120.survey_service.entity.*;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.*;
import com.trinhcong1120.survey_service.security.GroupPermissions;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class GoogleSurveyService {

    private static final String SHARE_CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern FORM_ID_PATTERN = Pattern.compile("/forms/d/(?:e/)?([a-zA-Z0-9_-]+)");

    private final SurveyRepository surveyRepo;
    private final SurveyIntegrationRepository integrationRepo;
    private final GoogleConnectionRepository connectionRepo;
    private final PageRepository pageRepo;
    private final GoogleFormsService formsService;
    private final GoogleDriveService driveService;
    private final SurveyAccessGuard accessGuard;
    private final GroupPermissions groupPermissions;
    private final CoreUserClient coreUserClient;
    private final SurveyScheduleService schedule;

    public GoogleSurveyService(
            SurveyRepository surveyRepo,
            SurveyIntegrationRepository integrationRepo,
            GoogleConnectionRepository connectionRepo,
            PageRepository pageRepo,
            GoogleFormsService formsService,
            GoogleDriveService driveService,
            SurveyAccessGuard accessGuard,
            GroupPermissions groupPermissions,
            CoreUserClient coreUserClient,
            SurveyScheduleService schedule
    ) {
        this.surveyRepo = surveyRepo;
        this.integrationRepo = integrationRepo;
        this.connectionRepo = connectionRepo;
        this.pageRepo = pageRepo;
        this.formsService = formsService;
        this.driveService = driveService;
        this.accessGuard = accessGuard;
        this.groupPermissions = groupPermissions;
        this.coreUserClient = coreUserClient;
        this.schedule = schedule;
    }

    public record CreateGoogleFormRequest(
            String title,
            String description,
            UUID groupId,
            UUID connectionId
    ) {}

    public record LinkGoogleFormRequest(
            String title,
            String description,
            UUID groupId,
            UUID connectionId,
            String googleFormUrlOrId
    ) {}

    public SurveyResponse createGoogleForm(CreateGoogleFormRequest request, String authHeader) {
        UUID currentUserId = accessGuard.userId();
        validateGroupPermission(request.groupId(), currentUserId);

        GoogleConnection connection = connectionRepo.findByIdAndUserId(request.connectionId(), currentUserId)
                .orElseThrow(() -> new BadRequestException("Tài khoản Google không tồn tại hoặc không thuộc quyền sở hữu của bạn."));

        String username = getUsername(authHeader);

        // 1. Create Google Form via Forms API
        Form form = formsService.createForm(connection, request.title(), request.description());
        String formId = form.getFormId();

        String editUrl = "https://docs.google.com/forms/d/" + formId + "/edit";
        String responseUrl = form.getResponderUri() != null ? form.getResponderUri() : "https://docs.google.com/forms/d/e/" + formId + "/viewform";

        // 2. Create Survey record
        Survey survey = new Survey();
        survey.setTitle(request.title());
        survey.setDescription(request.description());
        survey.setGroupId(request.groupId());
        survey.setCreatorUserId(currentUserId);
        survey.setCreatorUser(username);
        survey.setShareCode(generateShareCode());
        survey.setCreatedAt(LocalDateTime.now());
        survey.setPublishedAt(Instant.now());
        survey.setOpenAt(null);
        survey.setCloseAt(null);
        survey.setManuallyLocked(false);
        survey.setRequiresUnlock(false);
        survey.setValidationRevision(1L);
        survey = surveyRepo.save(survey);

        // 3. Create default Page
        Page page = new Page();
        page.setSurvey(survey);
        page.setTitle(request.title());
        page.setOrderIndex(1);
        pageRepo.save(page);

        // 4. Create SurveyIntegration
        SurveyIntegration integration = new SurveyIntegration();
        integration.setSurveyId(survey.getId());
        integration.setProvider("GOOGLE_FORMS");
        integration.setGoogleFormId(formId);
        integration.setOwnerConnectionId(connection.getId());
        integration.setEditUrl(editUrl);
        integration.setResponseUrl(responseUrl);
        integration.setSyncStatus("IDLE");
        integration = integrationRepo.save(integration);

        return toSurveyResponse(survey, integration);
    }

    public SurveyResponse linkGoogleForm(LinkGoogleFormRequest request, String authHeader) {
        UUID currentUserId = accessGuard.userId();
        validateGroupPermission(request.groupId(), currentUserId);

        String formId = extractFormId(request.googleFormUrlOrId());
        if (formId == null || formId.isBlank()) {
            throw new BadRequestException("URL hoặc Form ID Google Forms không hợp lệ.");
        }

        // Check if form is already linked
        Optional<SurveyIntegration> existingInteg = integrationRepo.findByGoogleFormId(formId);
        if (existingInteg.isPresent()) {
            throw new BadRequestException("Google Form này đã được liên kết với một khảo sát khác trong hệ thống.");
        }

        GoogleConnection connection = connectionRepo.findByIdAndUserId(request.connectionId(), currentUserId)
                .orElseThrow(() -> new BadRequestException("Tài khoản Google không tồn tại hoặc không thuộc quyền sở hữu của bạn."));

        // Verify access to the form
        Form form = formsService.getForm(connection, formId);

        String formTitle = (form.getInfo() != null && form.getInfo().getTitle() != null && !form.getInfo().getTitle().isBlank())
                ? form.getInfo().getTitle()
                : request.title();

        String formDesc = (form.getInfo() != null && form.getInfo().getDescription() != null)
                ? form.getInfo().getDescription()
                : request.description();

        String editUrl = "https://docs.google.com/forms/d/" + formId + "/edit";
        String responseUrl = form.getResponderUri() != null ? form.getResponderUri() : "https://docs.google.com/forms/d/e/" + formId + "/viewform";

        String username = getUsername(authHeader);

        // Create Survey
        Survey survey = new Survey();
        survey.setTitle(formTitle != null && !formTitle.isBlank() ? formTitle : "Khảo sát Google Form");
        survey.setDescription(formDesc);
        survey.setGroupId(request.groupId());
        survey.setCreatorUserId(currentUserId);
        survey.setCreatorUser(username);
        survey.setShareCode(generateShareCode());
        survey.setCreatedAt(LocalDateTime.now());
        survey.setPublishedAt(Instant.now());
        survey.setOpenAt(null);
        survey.setCloseAt(null);
        boolean isAccepting = formsService.isFormAcceptingResponses(connection, formId, responseUrl);
        survey.setManuallyLocked(!isAccepting);
        survey.setRequiresUnlock(false);
        survey.setValidationRevision(1L);
        survey = surveyRepo.save(survey);

        // Create SurveyIntegration
        SurveyIntegration integration = new SurveyIntegration();
        integration.setSurveyId(survey.getId());
        integration.setProvider("GOOGLE_FORMS");
        integration.setGoogleFormId(formId);
        integration.setOwnerConnectionId(connection.getId());
        integration.setEditUrl(editUrl);
        integration.setResponseUrl(responseUrl);
        integration.setSyncStatus("IDLE");
        integration = integrationRepo.save(integration);

        // Pull schema automatically so questions are available immediately
        try {
            formsService.pullFormSchema(survey, connection, formId);
        } catch (Exception e) {
            // Non-fatal if pulling schema fails immediately, user can pull later
        }

        return toSurveyResponse(survey, integration);
    }

    public void pullSchema(UUID surveyId) {
        accessGuard.require(surveyId, "EDIT");
        Survey survey = accessGuard.survey(surveyId);

        SurveyIntegration integration = integrationRepo.findBySurveyId(surveyId)
                .orElseThrow(() -> new BadRequestException("Khảo sát này không phải là Google Form"));

        GoogleConnection ownerConnection = null;
        if (integration.getOwnerConnectionId() != null) {
            ownerConnection = connectionRepo.findById(integration.getOwnerConnectionId()).orElse(null);
        }
        if (ownerConnection == null || !"ACTIVE".equals(ownerConnection.getStatus())) {
            if (survey.getCreatorUserId() != null) {
                ownerConnection = connectionRepo.findByUserId(survey.getCreatorUserId()).stream()
                        .filter(c -> "ACTIVE".equals(c.getStatus()))
                        .findFirst()
                        .orElse(null);
            }
        }
        if (ownerConnection == null) {
            throw new BadRequestException("Chủ sở hữu Form chưa kết nối tài khoản Google hợp lệ.");
        }

        formsService.pullFormSchema(survey, ownerConnection, integration.getGoogleFormId());

        boolean isAccepting = formsService.isFormAcceptingResponses(ownerConnection, integration.getGoogleFormId(), integration.getResponseUrl());
        survey.setManuallyLocked(!isAccepting);
        survey.setRequiresUnlock(false);
        survey.setOpenAt(null);
        survey.setCloseAt(null);
        surveyRepo.save(survey);
    }

    public GoogleIntegrationDto getSyncStatus(UUID surveyId) {
        accessGuard.require(surveyId, "VIEW");
        Survey survey = accessGuard.survey(surveyId);
        SurveyIntegration integration = integrationRepo.findBySurveyId(surveyId)
                .orElseThrow(() -> new BadRequestException("Khảo sát này không phải là Google Form"));

        GoogleIntegrationDto dto = new GoogleIntegrationDto();
        dto.setId(integration.getId());
        dto.setProvider(integration.getProvider());
        dto.setGoogleFormId(integration.getGoogleFormId());
        dto.setEditUrl(integration.getEditUrl());
        dto.setResponseUrl(integration.getResponseUrl());
        dto.setSyncStatus(integration.getSyncStatus());
        dto.setLastSyncedAt(integration.getLastSyncedAt());
        dto.setLastSyncError(integration.getLastSyncError());
        dto.setIsAcceptingResponses(!survey.isManuallyLocked());
        return dto;
    }

    private void validateGroupPermission(UUID groupId, UUID currentUserId) {
        if (groupId == null) {
            throw new BadRequestException("Vui lòng chọn nhóm khảo sát.");
        }
        var groupPerms = groupPermissions.group(groupId, currentUserId);
        if (!groupPerms.contains("CREATE_SURVEY") && !accessGuard.has("survey_create_all")) {
            throw new BadRequestException("Bạn không có quyền tạo khảo sát trong nhóm này.");
        }
    }

    private String getUsername(String authHeader) {
        try {
            CoreUserResponse me = coreUserClient.getMe(authHeader);
            if (me != null && me.getUsername() != null && !me.getUsername().isBlank()) {
                return me.getUsername();
            }
        } catch (Exception ignored) {}
        return "user";
    }

    private String generateShareCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(12);
            for (int j = 0; j < 12; j++) {
                sb.append(SHARE_CODE_CHARS.charAt(RANDOM.nextInt(SHARE_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!surveyRepo.existsByShareCode(code)) {
                return code;
            }
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String extractFormId(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        Matcher matcher = FORM_ID_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // If it's a raw Form ID (alphanumeric, dashes, underscores)
        if (trimmed.matches("^[a-zA-Z0-9_-]{15,100}$")) {
            return trimmed;
        }
        return trimmed;
    }

    private SurveyResponse toSurveyResponse(Survey survey, SurveyIntegration integ) {
        SurveyResponse response = new SurveyResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getCreatorUser(),
                survey.getCreatedAt(),
                schedule.status(survey).name()
        );
        response.setCreatorUserId(survey.getCreatorUserId());
        var actions = accessGuard.actions(survey);
        response.setCanEdit(actions.canEdit());
        response.setCanDelete(actions.canDelete());
        response.setCanManageAccess(actions.canManageAccess());
        response.setGroupId(survey.getGroupId());
        response.setCanViewResponses(accessGuard.can(survey, "VIEW_RESPONSES"));
        response.setCanExportData(accessGuard.can(survey, "EXPORT_DATA"));
        response.setCanManagePublication(false);
        response.setShareCode(survey.getShareCode());
        response.setOpenAt(null);
        response.setCloseAt(null);

        if (integ != null) {
            GoogleIntegrationDto dto = new GoogleIntegrationDto();
            dto.setId(integ.getId());
            dto.setProvider(integ.getProvider());
            dto.setGoogleFormId(integ.getGoogleFormId());
            dto.setEditUrl(integ.getEditUrl());
            dto.setResponseUrl(integ.getResponseUrl());
            dto.setSyncStatus(integ.getSyncStatus());
            dto.setLastSyncedAt(integ.getLastSyncedAt());
            dto.setLastSyncError(integ.getLastSyncError());
            dto.setIsAcceptingResponses(!survey.isManuallyLocked());
            response.setGoogleIntegration(dto);
        }

        return response;
    }

    public List<GoogleDriveFormItem> listAvailableGoogleForms(UUID connectionId, int pageSize) {
        UUID currentUserId = accessGuard.userId();
        GoogleConnection connection = connectionRepo.findByIdAndUserId(connectionId, currentUserId)
                .orElseThrow(() -> new BadRequestException("Tài khoản Google không tồn tại hoặc không thuộc quyền sở hữu của bạn."));

        List<File> files = driveService.listFormFiles(connection, pageSize);
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> formIds = files.stream().map(File::getId).filter(Objects::nonNull).toList();
        Map<String, UUID> linkedMap = new HashMap<>();
        if (!formIds.isEmpty()) {
            List<SurveyIntegration> linkedIntegrations = integrationRepo.findByGoogleFormIdIn(formIds);
            for (SurveyIntegration integ : linkedIntegrations) {
                linkedMap.put(integ.getGoogleFormId(), integ.getSurveyId());
            }
        }

        return files.stream().map(f -> {
            String formId = f.getId();
            String name = (f.getName() != null && !f.getName().isBlank()) ? f.getName() : "Biểu mẫu không có tiêu đề";
            String editUrl = "https://docs.google.com/forms/d/" + formId + "/edit";
            String webViewLink = f.getWebViewLink() != null ? f.getWebViewLink() : editUrl;
            Instant modified = f.getModifiedTime() != null ? Instant.ofEpochMilli(f.getModifiedTime().getValue()) : null;
            UUID linkedSurveyId = linkedMap.get(formId);
            return new GoogleDriveFormItem(
                    formId,
                    name,
                    editUrl,
                    webViewLink,
                    modified,
                    linkedSurveyId != null,
                    linkedSurveyId
            );
        }).toList();
    }
}
