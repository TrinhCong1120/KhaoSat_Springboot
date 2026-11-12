package com.trinhcong1120.survey_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.trinhcong1120.survey_service.entity.GoogleConnection;
import com.trinhcong1120.survey_service.exception.BadRequestException;
import com.trinhcong1120.survey_service.repository.GoogleConnectionRepository;
import com.trinhcong1120.survey_service.util.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    private static final List<String> SCOPES = List.of(
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/forms.body",
            "https://www.googleapis.com/auth/forms.responses.readonly",
            // drive.readonly: cho phép list tất cả files/forms trong Drive của user (bao gồm cả forms có sẵn).
            // drive.file (trước đây) chỉ cho phép xem files do app tạo — quá hạn hẹp.
            "https://www.googleapis.com/auth/drive.readonly"
    );

    private final GoogleConnectionRepository connectionRepo;
    private final TokenEncryptionService encryptionService;
    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    @Value("${app.google.client-id:}")
    private String clientId;

    @Value("${app.google.client-secret:}")
    private String clientSecret;

    @Value("${app.google.redirect-uri:http://localhost/api/integrations/google/callback}")
    private String redirectUri;

    @Value("${app.google.frontend-success-url:http://localhost:3000/admin/survey?google_connected=true}")
    private String frontendSuccessUrl;

    @Value("${app.google.frontend-failure-url:http://localhost:3000/admin/survey?google_error=true}")
    private String frontendFailureUrl;

    public GoogleOAuthService(GoogleConnectionRepository connectionRepo, TokenEncryptionService encryptionService) {
        this.connectionRepo = connectionRepo;
        this.encryptionService = encryptionService;
    }

    public String generateAuthUrl(UUID userId) {
        return generateAuthUrl(userId, null);
    }

    public String generateAuthUrl(UUID userId, String origin) {
        if (clientId == null || clientId.isBlank()) {
            throw new BadRequestException("Google Client ID chưa được cấu hình trên server.");
        }
        String originPart = (origin != null && !origin.isBlank())
                ? Base64.getUrlEncoder().withoutPadding().encodeToString(origin.trim().getBytes(StandardCharsets.UTF_8))
                : "";
        String stateRaw = userId.toString() + ":" + UUID.randomUUID() + ":" + Instant.now().toEpochMilli() + ":" + originPart;
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateRaw.getBytes(StandardCharsets.UTF_8));

        String scopesStr = String.join(" ", SCOPES);
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scopesStr, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    public GoogleConnection handleCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Mã authorization code không hợp lệ.");
        }
        UUID userId = parseUserIdFromState(state);

        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    httpTransport,
                    jsonFactory,
                    "https://oauth2.googleapis.com/token",
                    clientId,
                    clientSecret,
                    code,
                    redirectUri
            ).execute();

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();

            // Fetch user info from Google
            HttpRequest request = httpTransport.createRequestFactory().buildGetRequest(
                    new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo")
            );
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            var response = request.execute();
            Map<String, Object> userInfo = jsonFactory.createJsonParser(response.getContent()).parse(Map.class);

            String googleSubject = (String) userInfo.get("sub");
            String googleEmail = (String) userInfo.get("email");

            if (googleEmail == null || googleSubject == null) {
                throw new BadRequestException("Không lấy được email hoặc danh tính Google của người dùng.");
            }

            Optional<GoogleConnection> existingOpt = connectionRepo.findByUserIdAndGoogleEmail(userId, googleEmail);
            GoogleConnection conn = existingOpt.orElseGet(GoogleConnection::new);
            conn.setUserId(userId);
            conn.setGoogleSubject(googleSubject);
            conn.setGoogleEmail(googleEmail);
            conn.setGrantedScopes(tokenResponse.getScope() != null ? tokenResponse.getScope() : String.join(" ", SCOPES));
            conn.setStatus("ACTIVE");
            conn.setUpdatedAt(Instant.now());

            if (refreshToken != null && !refreshToken.isBlank()) {
                conn.setRefreshTokenEncrypted(encryptionService.encrypt(refreshToken));
            } else if (conn.getRefreshTokenEncrypted() == null) {
                throw new BadRequestException("Không nhận được refresh token từ Google. Vui lòng kết nối lại và cấp quyền.");
            }

            return connectionRepo.save(conn);
        } catch (Exception e) {
            log.error("Google OAuth token exchange failed: {}", e.getMessage(), e);
            throw new BadRequestException("Kết nối tài khoản Google thất bại: " + e.getMessage());
        }
    }

    public String getValidAccessToken(GoogleConnection connection) {
        if (connection == null) {
            throw new BadRequestException("Không tìm thấy kết nối Google.");
        }
        String refreshToken = encryptionService.decrypt(connection.getRefreshTokenEncrypted());
        if (refreshToken == null || refreshToken.isBlank()) {
            connection.setStatus("NEEDS_REAUTH");
            connectionRepo.save(connection);
            throw new BadRequestException("Tài khoản Google cần xác thực lại (thiếu refresh token).");
        }

        try {
            GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                    httpTransport,
                    jsonFactory,
                    refreshToken,
                    clientId,
                    clientSecret
            ).execute();

            return tokenResponse.getAccessToken();
        } catch (Exception e) {
            log.error("Failed to refresh Google access token for user {}: {}", connection.getGoogleEmail(), e.getMessage());
            connection.setStatus("NEEDS_REAUTH");
            connectionRepo.save(connection);
            throw new BadRequestException("Phiên làm việc Google đã hết hạn hoặc bị thu hồi. Vui lòng kết nối lại tài khoản " + connection.getGoogleEmail());
        }
    }

    public List<GoogleConnection> getUserConnections(UUID userId) {
        return connectionRepo.findByUserId(userId);
    }

    public void disconnect(UUID userId, UUID connectionId) {
        GoogleConnection connection = connectionRepo.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new BadRequestException("Kết nối Google không tồn tại hoặc không thuộc quyền quản lý."));
        connectionRepo.delete(connection);
    }

    public String getFrontendSuccessUrl() { return frontendSuccessUrl; }
    public String getFrontendFailureUrl() { return frontendFailureUrl; }

    public String resolveSuccessUrl(String state) {
        String origin = parseOriginFromState(state);
        if (origin != null && !origin.isBlank()) {
            return origin.replaceAll("/+$", "") + "/admin/survey?google_connected=true";
        }
        return frontendSuccessUrl;
    }

    public String resolveFailureUrl(String state, String reason) {
        String origin = parseOriginFromState(state);
        String base = (origin != null && !origin.isBlank())
                ? (origin.replaceAll("/+$", "") + "/admin/survey?google_error=true")
                : frontendFailureUrl;
        return base + (reason != null ? "&reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8) : "");
    }

    public String parseOriginFromState(String state) {
        if (state == null || state.isBlank()) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(state);
            String raw = new String(bytes, StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length >= 4 && !parts[3].isBlank()) {
                return new String(Base64.getUrlDecoder().decode(parts[3]), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private UUID parseUserIdFromState(String state) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return UUID.fromString(parts[0]);
        } catch (Exception e) {
            throw new BadRequestException("State OAuth không hợp lệ hoặc đã hết hạn.");
        }
    }
}
