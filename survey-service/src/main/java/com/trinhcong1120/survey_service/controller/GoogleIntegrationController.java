package com.trinhcong1120.survey_service.controller;

import com.trinhcong1120.survey_service.entity.GoogleConnection;
import com.trinhcong1120.survey_service.security.SurveyAccessGuard;
import com.trinhcong1120.survey_service.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/google")
public class GoogleIntegrationController {

    private final GoogleOAuthService oAuthService;
    private final SurveyAccessGuard accessGuard;

    public GoogleIntegrationController(GoogleOAuthService oAuthService, SurveyAccessGuard accessGuard) {
        this.oAuthService = oAuthService;
        this.accessGuard = accessGuard;
    }

    public record ConnectionResponse(
            UUID id,
            String googleEmail,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ConnectionResponse fromEntity(GoogleConnection c) {
            return new ConnectionResponse(
                    c.getId(),
                    c.getGoogleEmail(),
                    c.getStatus(),
                    c.getCreatedAt(),
                    c.getUpdatedAt()
            );
        }
    }

    @GetMapping("/connect-url")
    public ResponseEntity<Map<String, String>> getConnectUrl(@RequestParam(required = false) String origin) {
        UUID userId = accessGuard.userId();
        String url = oAuthService.generateAuthUrl(userId, origin);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        if (error != null || code == null || state == null) {
            String reason = error != null ? error : "Thiếu mã xác thực (code) hoặc state từ Google.";
            String fallbackUrl = oAuthService.resolveFailureUrl(state, reason);
            writeHtmlResponse(response, false, reason, fallbackUrl);
            return;
        }
        try {
            oAuthService.handleCallback(code, state);
            String successUrl = oAuthService.resolveSuccessUrl(state);
            writeHtmlResponse(response, true, "Đã kết nối tài khoản Google thành công!", successUrl);
        } catch (Exception e) {
            String fallbackUrl = oAuthService.resolveFailureUrl(state, e.getMessage());
            writeHtmlResponse(response, false, e.getMessage(), fallbackUrl);
        }
    }

    private void writeHtmlResponse(HttpServletResponse response, boolean success, String message, String fallbackUrl) throws IOException {
        String msgType = success ? "GOOGLE_OAUTH_SUCCESS" : "GOOGLE_OAUTH_ERROR";
        String escapedMsg = (message != null ? message : "").replace("\"", "\\\"").replace("'", "\\'");
        String title = success ? "Kết nối Google thành công" : "Kết nối Google thất bại";
        String iconBg = success ? "#10b981" : "#ef4444";
        String iconSymbol = success ? "&#10003;" : "&#10007;";
        String displayMsg = success ? "Đã liên kết tài khoản Google thành công. Bạn có thể sử dụng biểu mẫu Google Forms ngay bây giờ." : (message != null ? message : "Không thể kết nối tài khoản Google.");

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                            margin: 0;
                            background: #f8fafc;
                            color: #1e293b;
                        }
                        .card {
                            background: white;
                            padding: 2rem;
                            border-radius: 1rem;
                            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
                            text-align: center;
                            max-width: 380px;
                            width: 90%%;
                        }
                        .icon {
                            width: 52px;
                            height: 52px;
                            margin: 0 auto 1rem;
                            border-radius: 50%%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 26px;
                            background: %s;
                            color: white;
                        }
                        h2 { color: #1e293b; margin: 0 0 0.5rem; font-size: 1.25rem; font-weight: 600; }
                        p { font-size: 0.875rem; color: #64748b; margin: 0 0 1.25rem; line-height: 1.4; }
                        .btn {
                            display: inline-block;
                            background: #2563eb;
                            color: white;
                            padding: 0.6rem 1.25rem;
                            border-radius: 0.5rem;
                            font-size: 0.875rem;
                            font-weight: 500;
                            border: none;
                            cursor: pointer;
                        }
                        .status { font-size: 0.75rem; color: #94a3b8; margin-top: 1rem; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">%s</div>
                        <h2>%s</h2>
                        <p>%s</p>
                        <button class="btn" onclick="window.close()">Đóng cửa sổ</button>
                        <div class="status">Cửa sổ sẽ tự đóng trong giây lát...</div>
                    </div>
                    <script>
                        (function() {
                            var payload = { type: '%s', success: %s, message: '%s' };
                            try {
                                if (window.opener && !window.opener.closed) {
                                    window.opener.postMessage(payload, '*');
                                }
                            } catch (e) {}

                            setTimeout(function() {
                                window.close();
                            }, 500);
                        })();
                    </script>
                </body>
                </html>
                """.formatted(title, iconBg, iconSymbol, title, displayMsg, msgType, success, escapedMsg);

        response.getWriter().write(html);
    }

    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionResponse>> getConnections() {
        UUID userId = accessGuard.userId();
        List<GoogleConnection> list = oAuthService.getUserConnections(userId);
        List<ConnectionResponse> responses = list.stream()
                .map(ConnectionResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Map<String, String>> disconnect(@PathVariable UUID id) {
        UUID userId = accessGuard.userId();
        oAuthService.disconnect(userId, id);
        return ResponseEntity.ok(Map.of("message", "Đã ngắt kết nối tài khoản Google thành công"));
    }
}
