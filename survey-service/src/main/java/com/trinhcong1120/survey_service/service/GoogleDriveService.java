package com.trinhcong1120.survey_service.service;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.trinhcong1120.survey_service.entity.GoogleConnection;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);

    private final GoogleOAuthService oAuthService;
    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    public GoogleDriveService(GoogleOAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    public Drive getDriveClient(GoogleConnection connection) {
        String accessToken = oAuthService.getValidAccessToken(connection);
        HttpRequestInitializer initializer = request -> request.getHeaders().setAuthorization("Bearer " + accessToken);
        return new Drive.Builder(httpTransport, jsonFactory, initializer)
                .setApplicationName("SurveyApp")
                .build();
    }

    public String grantWriterPermission(GoogleConnection ownerConnection, String fileId, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new BadRequestException("Email người nhận quyền không hợp lệ.");
        }
        try {
            Drive drive = getDriveClient(ownerConnection);

            // Check if already has writer permission
            try {
                PermissionList list = drive.permissions().list(fileId)
                        .setFields("permissions(id, role, emailAddress, type)")
                        .execute();
                List<Permission> permissions = list.getPermissions();
                if (permissions != null) {
                    for (Permission p : permissions) {
                        if (recipientEmail.equalsIgnoreCase(p.getEmailAddress())) {
                            if ("writer".equalsIgnoreCase(p.getRole()) || "owner".equalsIgnoreCase(p.getRole())) {
                                log.info("User {} already has {} role on file {}", recipientEmail, p.getRole(), fileId);
                                return p.getId();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not list permissions on file {}: {}", fileId, e.getMessage());
            }

            // Create writer permission
            Permission newPermission = new Permission();
            newPermission.setType("user");
            newPermission.setRole("writer");
            newPermission.setEmailAddress(recipientEmail);

            Permission created = drive.permissions().create(fileId, newPermission)
                    .setSendNotificationEmail(false)
                    .execute();

            log.info("Granted writer permission to {} on Google file {}", recipientEmail, fileId);
            return created.getId();
        } catch (Exception e) {
            log.error("Failed to grant writer permission on Google Drive file {}: {}", fileId, e.getMessage(), e);
            throw new BadRequestException("Không thể cấp quyền chỉnh sửa trên Google Drive: " + e.getMessage());
        }
    }

    public List<File> listFormFiles(GoogleConnection connection, int pageSize) {
        try {
            Drive drive = getDriveClient(connection);
            FileList result = drive.files().list()
                    .setQ("mimeType = 'application/vnd.google-apps.form' and trashed = false")
                    .setPageSize(pageSize > 0 ? Math.min(pageSize, 100) : 50)
                    .setOrderBy("modifiedTime desc")
                    .setFields("files(id, name, modifiedTime, webViewLink)")
                    .execute();

            return result.getFiles() != null ? result.getFiles() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not list Google Drive forms for connection {}: {}", connection.getId(), e.getMessage(), e);
            // Throw exception rõ ràng để frontend biết lý do thực sự (hết token, thiếu scope, lỗi Google API)
            String reason = e.getMessage();
            if (reason != null && reason.contains("403")) {
                throw new BadRequestException(
                    "Không có quyền truy cập Google Drive. " +
                    "Vui lòng ngắt kết nối và kết nối lại tài khoản Google để cấp lại quyền."
                );
            } else if (reason != null && reason.contains("401")) {
                throw new BadRequestException(
                    "Phiên làm việc Google đã hết hạn. " +
                    "Vui lòng ngắt kết nối và kết nối lại tài khoản Google."
                );
            } else {
                throw new BadRequestException(
                    "Không thể tải danh sách biểu mẫu từ Google Drive: " + reason
                );
            }
        }
    }
}
