package com.trinhcong1120.notification_service.service;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ZaloService {

    private static final Logger log = LoggerFactory.getLogger(ZaloService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final String token;
    private final String webhookUrl;
    private final String webhookSecret;
    private final JdbcTemplate jdbc;
    private final RestClient client;
    private final BotInteractionService botService;
    private InboxStore inbox;

    @Autowired(required = false)
    public void setInbox(InboxStore inbox) { this.inbox = inbox; }

    @Autowired
    public ZaloService(@Value("${spring.zalo.bot.token:}") String token,
                       @Value("${spring.zalo.bot.webhook-url:}") String webhookUrl,
                       @Value("${spring.zalo.bot.webhook-secret:}") String webhookSecret,
                       JdbcTemplate jdbc,
                       @Autowired(required = false) BotInteractionService botService) {
        this(token, webhookUrl, webhookSecret, jdbc,
                botService != null ? botService : new BotInteractionService(jdbc),
                createClient(token));
    }

    ZaloService(String token, String webhookUrl, String webhookSecret,
                JdbcTemplate jdbc, RestClient client) {
        this(token, webhookUrl, webhookSecret, jdbc, new BotInteractionService(jdbc), client);
    }

    ZaloService(String token, String webhookUrl, String webhookSecret,
                JdbcTemplate jdbc, BotInteractionService botService, RestClient client) {
        this.token = token == null ? "" : token.trim();
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.jdbc = jdbc;
        this.botService = botService;
        this.client = client;
    }

    private static RestClient createClient(String token) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(10_000);
        requests.setReadTimeout(35_000);
        return RestClient.builder()
                .baseUrl("https://bot-api.zaloplatforms.com/bot" + (token == null ? "" : token.trim()))
                .requestFactory(requests).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        if (token.isBlank() || webhookUrl.isBlank() || webhookSecret.isBlank()) {
            log.warn("Zalo webhook disabled: configure ZALO_BOT_TOKEN, ZALO_WEBHOOK_URL and ZALO_WEBHOOK_SECRET");
            return;
        }
        if (!webhookUrl.startsWith("https://") || !webhookSecret.matches("[A-Za-z0-9_-]{8,256}"))
            throw new IllegalStateException("Invalid Zalo webhook URL or secret");
        try {
            ensureOk(client.post().uri("/setWebhook")
                    .body(Map.of("url", webhookUrl, "secret_token", webhookSecret))
                    .retrieve().body(JsonNode.class), "setWebhook");
        } catch (RuntimeException ex) {
            log.error("Could not register Zalo webhook", ex);
        }
    }

    public String webhookSecret() { return webhookSecret; }

    public void handleUpdate(JsonNode update) {
        JsonNode result = update.path("result");
        if (result.isMissingNode() || result.isNull()) result = update;
        JsonNode message = result.path("message");

        String eventName = update.path("event_name").asText("");
        if (eventName.isBlank()) eventName = update.path("event").asText("");

        String chatId = "";
        String messageId = "";
        long timestamp = 0;
        String raw = "";

        if (!message.isMissingNode() && !message.isNull()) {
            JsonNode chat = message.path("chat");
            String chatType = chat.path("chat_type").asText("");
            if (!chatType.isBlank() && !"PRIVATE".equalsIgnoreCase(chatType)) return;
            chatId = chat.path("id").asText("").trim();
            if (chatId.isBlank()) {
                chatId = message.path("from").path("id").asText("").trim();
            }
            messageId = message.path("message_id").asText("");
            if (messageId.isBlank()) messageId = message.path("msg_id").asText("");
            timestamp = message.path("date").asLong(0);
            raw = message.path("text").asText("").trim();
            if (raw.isBlank()) raw = message.path("caption").asText("").trim();
        } else if (!eventName.isBlank()) {
            // Hỗ trợ các webhook sự kiện Zalo OA/Platform như follow, start_chat, open_chat, v.v.
            chatId = update.path("sender").path("id").asText("").trim();
            if (chatId.isBlank()) chatId = update.path("user_id_by_app").asText("").trim();
            if (chatId.isBlank()) chatId = update.path("chat").path("id").asText("").trim();
            if (chatId.isBlank()) chatId = update.path("from").path("id").asText("").trim();

            messageId = update.path("msg_id").asText("");
            if (messageId.isBlank()) messageId = update.path("message_id").asText("");
            timestamp = update.path("timestamp").asLong(0);

            if ("follow".equalsIgnoreCase(eventName) || "start_chat".equalsIgnoreCase(eventName)
                    || "open_chat".equalsIgnoreCase(eventName) || "start".equalsIgnoreCase(eventName)) {
                raw = "/start";
            } else if (update.has("message") && update.path("message").has("text")) {
                raw = update.path("message").path("text").asText("").trim();
            } else if (update.has("info") && update.path("info").has("message")) {
                raw = update.path("info").path("message").asText("").trim();
            }
        }

        if (chatId.isBlank()) return;
        String messageKey = messageId.isBlank()
                ? chatId + ":" + (timestamp > 0 ? timestamp : Instant.now().toEpochMilli())
                : messageId;
        String command = raw.isBlank() ? "" : raw.split("\\s+", 2)[0].split("@", 2)[0].toLowerCase();
        String argument = raw.contains(" ") ? raw.substring(raw.indexOf(' ') + 1).trim() : "";

        // Kiểm tra tránh xử lý trùng lặp tin nhắn
        try {
            List<String> existing = jdbc.query(
                    "SELECT status FROM zalo_chat_messages WHERE message_key = ? AND direction = 'OUT'",
                    (rs, rowNum) -> rs.getString("status"), messageKey);
            if (!existing.isEmpty() && "SENT".equals(existing.get(0))) {
                log.info("Duplicate Zalo message {} already processed, skipping", messageKey);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to check duplicate message for chat {}", chatId, ex);
        }

        if (inbox != null && !raw.isBlank()) {
            long epochMs = timestamp;
            Instant receivedAt = epochMs > 1_000_000_000_000L
                    ? Instant.ofEpochMilli(epochMs)
                    : epochMs > 0 ? Instant.ofEpochSecond(epochMs) : Instant.now();
            inbox.save("ZALO", "zalo:" + messageKey, messageId.isBlank() ? null : messageId,
                    chatId, "bot", null, raw,
                    command.startsWith("/") ? "COMMAND" : "MESSAGE", receivedAt, null);
        }

        BotInteractionService.BotAccount account = botService.getAccountByChatId("ZALO", chatId);

        // Xử lý deep-linking: /start link_123456
        if (command.equals("/start") && argument.startsWith("link_")) {
            String otp = argument.substring("link_".length()).trim();
            command = "/link";
            argument = otp;
        }

        BotResponse botResponse;
        try {
            botResponse = processAction(command, argument, chatId, account);
        } catch (Exception ex) {
            log.error("Error processing Zalo action {} for chat {}", command, chatId, ex);
            botResponse = new BotResponse("ERROR", "⚠️ Đã có lỗi xảy ra khi xử lý yêu cầu. Vui lòng thử lại sau giây lát.");
        }

        try {
            jdbc.update("""
                    INSERT INTO zalo_chat_messages (message_key, chat_id, account_user_id, direction, action, content, status)
                    VALUES (?, ?, ?, 'IN', ?, ?, 'RECEIVED') ON CONFLICT (message_key, direction) DO NOTHING
                    """, messageKey, chatId, account == null ? null : account.id(), botResponse.action(), raw);
        } catch (Exception ex) {
            log.warn("Failed to record incoming zalo message for chat {}", chatId, ex);
        }

        try {
            if (botResponse.extraMessages() != null && !botResponse.extraMessages().isEmpty()) {
                for (String extra : botResponse.extraMessages()) {
                    sendMessage(chatId, extra);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            sendMessage(chatId, botResponse.text());
            String loggedContent = (botResponse.extraMessages() == null || botResponse.extraMessages().isEmpty())
                    ? botResponse.text()
                    : String.join("\n\n---\n\n", botResponse.extraMessages()) + "\n\n---\n\n" + botResponse.text();
            logReply(messageKey, chatId, account, botResponse.action(), loggedContent, "SENT", null);
        } catch (RuntimeException ex) {
            logReply(messageKey, chatId, account, botResponse.action(), botResponse.text(), "FAILED", ex.getMessage());
            log.error("Failed to send zalo reply to chat {}", chatId, ex);
        }
    }

    private record BotResponse(String action, String text, List<String> extraMessages) {
        BotResponse(String action, String text) {
            this(action, text, List.of());
        }
    }

    private BotResponse processAction(String command, String argument, String chatId,
                                      BotInteractionService.BotAccount account) {
        if (command.startsWith("/ks_")) {
            return handleSurveyDetail(account, command.substring(4));
        }

        return switch (command) {
            case "/start", "start" -> handleStart(account, chatId);
            case "/thongke", "/stats", "stats" -> handleStats(account);
            case "/khaosat", "/surveys" -> {
                int page = 0;
                try { if (!argument.isBlank()) page = Math.max(0, Integer.parseInt(argument) - 1); } catch (Exception ignored) {}
                yield handleSurveys(account, page);
            }
            case "/ks" -> handleSurveyDetail(account, argument);
            case "/hethong", "/admin" -> handleAdminSystem(account);
            case "/canhbao" -> handleAdminFailedCases(account);
            case "/thongtin", "/info", "info" -> handleInfo(account, chatId);
            case "/getid", "getid" -> handleGetId(chatId);
            case "/dangky", "/register", "register", "link_guide" -> handleRegisterGuide(account, chatId);
            case "/link", "/lienket" -> handleLinkCode(account, argument, chatId);
            case "/unlink", "/huylienket", "/huy" -> handleUnlink(chatId);
            case "/help", "/trogiup", "/huongdan", "help" -> handleHelp(account);
            default -> handleStart(account, chatId);
        };
    }

    private BotResponse handleStart(BotInteractionService.BotAccount account, String chatId) {
        if (account == null) {
            String welcomeMsg = """
                    👋 CHÀO MỪNG BẠN ĐẾN VỚI BOT KHẢO SÁT & BÁO CÁO ZALO!
                    ─────────────────────────
                    🤖 Bot sẽ đồng hành cùng bạn để:
                    📊 Theo dõi số liệu thống kê & phản hồi theo thời gian thực
                    📋 Tra cứu danh sách & xem chi tiết các bài khảo sát
                    🔔 Nhận thông báo tức thì ngay khi có người gửi bài khảo sát
                    🛠 Quản trị viên: Giám sát toàn hệ thống & cảnh báo bài nộp lỗi""";

            String guideMsg = """
                    🚀 HƯỚNG DẪN BẮT ĐẦU SỬ DỤNG:
                    ─────────────────────────
                    Chat Zalo này chưa được liên kết với hệ thống khảo sát trên Web.
                    Hãy thực hiện các bước sau để kết nối:

                    1️⃣ Đăng nhập vào Website Khảo sát.
                    2️⃣ Bấm vào nút 🤖 "Bot thông báo" trên thanh Header.
                    3️⃣ Chọn Zalo Bot để lấy Mã OTP 6 chữ số (hiệu lực 10 phút).
                    4️⃣ Gửi mã cho Bot theo cú pháp:
                       👉 /link <mã_OTP>   (Ví dụ: /link 123456)
                       (Hoặc bấm nút "Mở Zalo liên kết tự động" trên Web)

                    📖 CẨM NANG CÁC CÂU LỆNH:
                    • /link : Hướng dẫn liên kết chi tiết
                    • /thongke : Xem báo cáo thống kê của bạn
                    • /khaosat : Danh sách bài khảo sát được phân quyền
                    • /thongtin : Kiểm tra tài khoản đã liên kết
                    • /getid : Lấy Chat ID của bạn để hỗ trợ thủ công
                    • /help : Mở hướng dẫn sử dụng chi tiết""";

            return new BotResponse("START", guideMsg, List.of(welcomeMsg));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("👋 Xin chào, %s!\n\nChào mừng bạn quay lại hệ thống khảo sát.\n\n", account.username()));
        sb.append("📋 Các lệnh hỗ trợ nhanh:\n");
        sb.append("• /thongke - Xem báo cáo thống kê của bạn\n");
        sb.append("• /khaosat - Danh sách khảo sát được phân quyền\n");
        sb.append("• /ks <mã_id> - Xem chi tiết bài khảo sát\n");
        sb.append("• /thongtin - Kiểm tra tài khoản đã liên kết\n");

        if (account.isAdmin()) {
            sb.append("\n🛠 Dành cho Quản trị viên:\n");
            sb.append("• /hethong - Thống kê toàn hệ thống\n");
            sb.append("• /canhbao - Danh sách khảo sát nộp lỗi\n");
        }

        sb.append("\n• /help - Xem hướng dẫn chi tiết");
        return new BotResponse("START", sb.toString());
    }

    private BotResponse handleStats(BotInteractionService.BotAccount account) {
        if (account == null) return requireLinkResponse();

        BotInteractionService.UserStatsDto stats = botService.getUserStats(account.id(), account.isAdmin());
        String text = String.format("""
                📊 BÁO CÁO THỐNG KÊ CỦA BẠN
                ─────────────────────────
                👤 Tài khoản: %s
                📋 Tổng số khảo sát: %d
                🟢 Khảo sát đang mở: %d
                💬 Tổng phản hồi nhận được: %d
                📅 Phản hồi mới hôm nay: %d
                ─────────────────────────
                👉 Gửi /khaosat để xem danh sách chi tiết.""",
                account.username(), stats.totalSurveys(), stats.activeSurveys(),
                stats.totalResponses(), stats.todayResponses());

        return new BotResponse("STATS", text);
    }

    private BotResponse handleSurveys(BotInteractionService.BotAccount account, int page) {
        if (account == null) return requireLinkResponse();

        int pageSize = 5;
        int total = botService.countUserSurveys(account.id(), account.isAdmin());
        List<BotInteractionService.SurveySummaryDto> surveys = botService.getUserSurveys(account.id(), account.isAdmin(), page, pageSize);

        if (surveys.isEmpty()) {
            return new BotResponse("SURVEYS", "📋 Bạn chưa có bài khảo sát nào được tạo hoặc được phân quyền truy cập.");
        }

        int maxPage = (int) Math.ceil((double) total / pageSize);
        StringBuilder sb = new StringBuilder();
        sb.append("📋 DANH SÁCH KHẢO SÁT\n");
        sb.append(String.format("Trang %d/%d • Tổng cộng: %d bài khảo sát\n", page + 1, maxPage, total));
        sb.append("═════════════════════════\n\n");

        int idx = page * pageSize + 1;
        for (int i = 0; i < surveys.size(); i++) {
            BotInteractionService.SurveySummaryDto s = surveys.get(i);
            String status = s.isActive() ? "🟢 Đang mở" : "🔴 Đang đóng";
            String codeTag = (s.shareCode() != null && !s.shareCode().isBlank()) ? " (Mã: " + s.shareCode() + ")" : "";

            sb.append(String.format("🔹 [%d] %s\n", idx, s.title()));
            sb.append(String.format("     Trạng thái: %s\n", status));
            sb.append(String.format("     Quy mô: 📄 %d trang  |  ❓ %d câu  |  💬 %d phản hồi\n",
                    s.pageCount(), s.questionCount(), s.responseCount()));
            sb.append(String.format("     👉 Xem chi tiết & Link: /ks %d%s\n", idx, codeTag));

            if (i < surveys.size() - 1) {
                sb.append("\n─────────────────────────\n\n");
            }
            idx++;
        }

        sb.append("\n═════════════════════════\n");
        sb.append("💡 HƯỚNG DẪN THAO TÁC:\n");
        sb.append(String.format("• Gõ /ks <số> (VD: /ks %d) để xem chi tiết & lấy link làm khảo sát\n", page * pageSize + 1));
        if (maxPage > 1) {
            int nextP = (page + 1) >= maxPage ? 1 : (page + 2);
            sb.append(String.format("• Gõ /khaosat <số_trang> (VD: /khaosat %d) để lật trang\n", nextP));
        }

        return new BotResponse("SURVEYS", sb.toString());
    }

    private BotResponse handleSurveyDetail(BotInteractionService.BotAccount account, String idStr) {
        if (account == null) return requireLinkResponse();

        if (idStr == null || idStr.isBlank()) {
            return new BotResponse("SURVEY_DETAIL", "❌ Vui lòng cung cấp số thứ tự hoặc mã khảo sát, ví dụ: /ks 1 hoặc /ks <mã>");
        }

        BotInteractionService.SurveyDetailDto d = botService.getSurveyByCodeOrId(account.id(), idStr.trim(), account.isAdmin());
        if (d == null) {
            return new BotResponse("SURVEY_DETAIL", "❌ Không tìm thấy khảo sát hoặc bạn không có quyền xem khảo sát này. Gửi /khaosat để xem danh sách.");
        }

        String status = d.isActive() ? "🟢 ĐANG HOẠT ĐỘNG" : "🔴 ĐÃ ĐÓNG / TẠM DỪNG";
        String desc = (d.description() == null || d.description().isBlank()) ? "(Không có mô tả)" : d.description();
        String createdDate = d.createdAt() != null ? DATE_TIME_FORMATTER.format(d.createdAt()) : "N/A";
        String shareRef = (d.shareCode() != null && !d.shareCode().isBlank()) ? d.shareCode() : d.id().toString();

        String text = String.format("""
                📋 CHI TIẾT BÀI KHẢO SÁT
                ═════════════════════════
                📌 Tiêu đề: %s
                📊 Trạng thái: %s
                📝 Mô tả: %s

                📈 THÔNG TIN THỐNG KÊ:
                • 📄 Quy mô: %d trang câu hỏi
                • ❓ Tổng số: %d câu hỏi
                • 💬 Thu thập: %d phản hồi
                • 📅 Ngày tạo: %s

                🔗 LIÊN KẾT LÀM KHẢO SÁT:
                http://localhost:3000/survey/%s
                (Sao chép link trên gửi cho người trả lời)
                ═════════════════════════
                👉 Gửi /khaosat để quay lại danh sách.""",
                d.title(), status, desc, d.pageCount(), d.questionCount(), d.responseCount(), createdDate, shareRef);

        return new BotResponse("SURVEY_DETAIL", text);
    }

    private BotResponse handleAdminSystem(BotInteractionService.BotAccount account) {
        if (account == null || !account.isAdmin()) {
            return new BotResponse("ADMIN", "⛔ Bạn không có quyền truy cập vào báo cáo quản trị hệ thống.");
        }

        BotInteractionService.AdminSystemStatsDto stats = botService.getAdminSystemStats();
        String text = String.format("""
                🛠 BÁO CÁO HỆ THỐNG TOÀN DIỆN (ADMIN)
                ─────────────────────────
                👥 Tổng người dùng: %d
                📋 Tổng số khảo sát: %d (Đang mở: %d)
                💬 Tổng phản hồi thu thập: %d
                📅 Phản hồi mới hôm nay: %d
                ⚠️ Bài nộp lỗi chờ xử lý: %d
                ─────────────────────────
                👉 Gửi /canhbao để xem chi tiết các khảo sát bị lỗi.""",
                stats.totalUsers(), stats.totalSurveys(), stats.activeSurveys(),
                stats.totalResponses(), stats.todayResponses(), stats.pendingFailedCases());

        return new BotResponse("ADMIN_STATS", text);
    }

    private BotResponse handleAdminFailedCases(BotInteractionService.BotAccount account) {
        if (account == null || !account.isAdmin()) {
            return new BotResponse("ADMIN", "⛔ Bạn không có quyền truy cập chức năng này.");
        }

        List<BotInteractionService.FailedCaseSummaryDto> cases = botService.getRecentFailedCases(5);
        if (cases.isEmpty()) {
            return new BotResponse("ADMIN_FAIL", "✅ Tuyệt vời! Hiện tại không có bài khảo sát nào bị lỗi gửi.");
        }

        StringBuilder sb = new StringBuilder("⚠️ DANH SÁCH BÀI NỘP KHẢO SÁT LỖI (Gần đây):\n─────────────────────────\n");
        for (BotInteractionService.FailedCaseSummaryDto c : cases) {
            String time = c.createdAt() != null ? DATE_TIME_FORMATTER.format(c.createdAt()) : "N/A";
            sb.append(String.format("• Tệp: %s\n  Trạng thái: %s | Cỡ: %d bytes\n  Thời gian: %s\n\n",
                    c.originalFileName(), c.status(), c.fileSize(), time));
        }

        return new BotResponse("ADMIN_FAIL", sb.toString());
    }

    private BotResponse handleInfo(BotInteractionService.BotAccount account, String chatId) {
        if (account == null) {
            return new BotResponse("INFO", "Chat này chưa liên kết với tài khoản hệ thống.\nChat ID của bạn: " + chatId);
        }

        String role = account.isAdmin() ? "Quản trị viên (Admin/Manager)" : "Người dùng / Khảo sát viên";
        String text = String.format("""
                👤 THÔNG TIN TÀI KHOẢN LIÊN KẾT
                ─────────────────────────
                Tên đăng nhập: %s
                Email: %s
                Vai trò: %s
                Zalo Chat ID: %s
                ─────────────────────────
                Để hủy liên kết chat này, gửi lệnh /huylienket""",
                account.username(), maskEmail(account.email()), role, chatId);

        return new BotResponse("INFO", text);
    }

    private BotResponse handleLinkCode(BotInteractionService.BotAccount account, String code, String chatId) {
        if (account != null && (code == null || code.isBlank())) {
            String role = account.isAdmin() ? "Quản trị viên (Admin/Manager)" : "Người dùng / Khảo sát viên";
            String text = String.format("""
                    ℹ️ BẠN ĐÃ LIÊN KẾT TÀI KHOẢN RỒI!
                    ─────────────────────────
                    👤 Tên tài khoản: %s
                    📧 Email: %s
                    🔑 Vai trò: %s
                    ─────────────────────────
                    Bạn đã có thể sử dụng các lệnh:
                    • /thongke : Xem số liệu thống kê cá nhân
                    • /khaosat : Xem danh sách khảo sát của bạn
                    
                    Nếu bạn muốn đổi sang tài khoản khác, hãy gửi: /huylienket rồi liên kết lại.""",
                    account.username(), maskEmail(account.email()), role);
            return new BotResponse("ALREADY_LINKED", text);
        }

        if (code == null || code.isBlank()) {
            return handleRegisterGuide(account, chatId);
        }

        String username = botService.linkAccountByCode(code.trim(), "ZALO", chatId);
        if (username != null) {
            String text = String.format("""
                    🎉 CHÚC MỪNG BẠN ĐÃ LIÊN KẾT THÀNH CÔNG!
                    ─────────────────────────
                    Xin chào %s! Tài khoản Zalo của bạn đã kết nối hoàn tất với hệ thống khảo sát.

                    🚀 HƯỚNG DẪN BẮT ĐẦU SỬ DỤNG:
                    1️⃣ /thongke : Xem báo cáo tổng phản hồi & tỷ lệ hoàn thành.
                    2️⃣ /khaosat : Xem danh sách khảo sát & số câu trả lời.
                    3️⃣ /ks <mã_id> : Xem thông tin chi tiết một bài khảo sát.
                    4️⃣ Nhận thông báo tự động: Khi có người nộp bài khảo sát, Bot sẽ nhắn tin báo ngay!
                    5️⃣ /help : Xem cẩm nang toàn bộ câu lệnh bất cứ lúc nào.""", username);
            return new BotResponse("LINK_SUCCESS", text);
        } else {
            String text = """
                    ❌ MÃ LIÊN KẾT KHÔNG CHÍNH XÁC HOẶC ĐÃ HẾT HẠN!
                    ─────────────────────────
                    Mã liên kết chỉ có hiệu lực trong 10 phút và chỉ sử dụng được 1 lần duy nhất.

                    👉 Cách lấy mã mới:
                    1. Đăng nhập Website Khảo sát.
                    2. Bấm nút "Bot thông báo" trên thanh Header.
                    3. Chọn tab "Zalo Bot" và bấm "Tạo mã mới".
                    4. Gửi lại vào đây theo cú pháp:
                       /link <mã_mới>
                    
                    Gõ /link để xem lại hướng dẫn chi tiết.""";
            return new BotResponse("LINK_FAILED", text);
        }
    }

    private BotResponse handleUnlink(String chatId) {
        boolean unlinked = botService.unlinkAccount("ZALO", chatId);
        String text = unlinked
                ? "✅ Đã hủy liên kết tài khoản khỏi chat này thành công.\nBạn có thể gửi /link bất cứ lúc nào để liên kết lại."
                : "Tài khoản hiện chưa được liên kết.";
        return new BotResponse("UNLINK", text);
    }

    private BotResponse handleGetId(String chatId) {
        return new BotResponse("GET_ID", "Chat ID Zalo của bạn: " + chatId + "\nGửi ID này cho quản trị viên để liên kết thủ công.");
    }

    private BotResponse handleRegisterGuide(BotInteractionService.BotAccount account, String chatId) {
        String text = String.format("""
                🔗 HƯỚNG DẪN LIÊN KẾT TÀI KHOẢN TỪ WEB QUA BOT ZALO
                ─────────────────────────
                Để Bot có thể gửi thông báo và hiển thị dữ liệu khảo sát của bạn, hãy thực hiện theo 4 bước sau:

                1️⃣ Bước 1: Mở trình duyệt và đăng nhập vào Website Khảo sát của bạn.
                
                2️⃣ Bước 2: Nhìn lên góc trên bên phải thanh Header, bấm vào nút:
                   🤖 "Bot thông báo"
                
                3️⃣ Bước 3: Cửa sổ liên kết hiện ra, chọn tab "Zalo Bot".
                   Hệ thống sẽ cấp cho bạn một Mã OTP gồm 6 chữ số (Ví dụ: 824915, có hiệu lực trong 10 phút).
                
                4️⃣ Bước 4: Quay lại khung chat Zalo này và gửi tin nhắn theo cú pháp:
                   👉 /link <mã_OTP>
                   (Ví dụ: /link 824915)

                ─────────────────────────
                Gặp khó khăn? Chat ID Zalo của bạn là: %s
                Bạn có thể gửi Chat ID này cho Quản trị viên để được hỗ trợ thủ công.""", chatId);
        return new BotResponse("LINK_GUIDE", text);
    }

    private BotResponse handleHelp(BotInteractionService.BotAccount account) {
        StringBuilder sb = new StringBuilder();
        sb.append("📖 CẨM NANG HƯỚNG DẪN SỬ DỤNG ZALO BOT KHẢO SÁT\n");
        sb.append("─────────────────────────\n\n");
        sb.append("🔹 LỆNH CƠ BẢN DÀNH CHO MỌI NGƯỜI DÙNG:\n");
        sb.append("• /start : Xem thông tin chào mừng và tóm tắt chức năng\n");
        sb.append("• /link : Hướng dẫn cách lấy mã kết nối từ Web qua Bot\n");
        sb.append("• /link <mã> : Kích hoạt liên kết tài khoản bằng mã OTP 6 số\n");
        sb.append("• /thongke : Báo cáo thống kê khảo sát và phản hồi của bạn\n");
        sb.append("• /khaosat : Xem danh sách khảo sát của bạn (hoặc /khaosat <trang>)\n");
        sb.append("• /ks <id> : Xem chi tiết một bài khảo sát\n");
        sb.append("• /thongtin : Kiểm tra tài khoản Web đang liên kết\n");
        sb.append("• /huylienket : Hủy liên kết tài khoản khỏi chat này\n");
        sb.append("• /getid : Xem Zalo Chat ID của bạn\n");
        sb.append("• /help : Xem cẩm nang hướng dẫn này\n");

        if (account != null && account.isAdmin()) {
            sb.append("\n🛠 DÀNH CHO QUẢN TRỊ VIÊN (ADMIN/MANAGER):\n");
            sb.append("• /hethong : Báo cáo thống kê toàn bộ hệ thống\n");
            sb.append("• /canhbao : Xem danh sách các bài khảo sát nộp bị lỗi\n");
        }

        return new BotResponse("HELP", sb.toString());
    }

    private BotResponse requireLinkResponse() {
        String text = """
                ⚠️ BẠN CHƯA LIÊN KẾT TÀI KHOẢN!
                ─────────────────────────
                Để xem số liệu thống kê và danh sách bài khảo sát của bạn, hãy liên kết tài khoản Web với Bot.
                
                👉 Gửi lệnh: /link để xem hướng dẫn 4 bước liên kết cực nhanh!
                👉 Nếu đã có mã OTP từ Web, hãy gửi ngay: /link <mã_OTP>""";
        return new BotResponse("REQUIRE_LINK", text);
    }

    private void logReply(String messageKey, String chatId, BotInteractionService.BotAccount account, String action,
                          String content, String status, String error) {
        jdbc.update("""
                INSERT INTO zalo_chat_messages
                (message_key, chat_id, account_user_id, direction, action, content, status, error_note)
                VALUES (?, ?, ?, 'OUT', ?, ?, ?, ?)
                ON CONFLICT (message_key, direction) DO UPDATE SET status = EXCLUDED.status, error_note = EXCLUDED.error_note
                """, messageKey, chatId, account == null ? null : account.id(), action, content,
                status, error == null ? null : error.substring(0, Math.min(error.length(), 500)));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "chưa có";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(1, at)) + "***" + email.substring(at);
    }

    public String sendNotification(String chatId, String subject, String message) {
        return sendNotification(chatId, subject, message, null);
    }

    public String sendNotification(String chatId, String subject, String message, String replyToId) {
        if (token.isBlank()) throw new IllegalStateException("Zalo bot token is not configured");
        return sendMessage(chatId, subject + "\n\n" + message);
    }

    private String sendMessage(String chatId, String text) {
        if (text.length() > 2000) text = text.substring(0, 2000);
        JsonNode response = client.post().uri("/sendMessage")
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve().body(JsonNode.class);
        ensureOk(response, "sendMessage");
        return response.path("result").path("message_id").isMissingNode() ? null
                : response.path("result").path("message_id").asText();
    }

    private void ensureOk(JsonNode response, String operation) {
        if (response == null || !response.path("ok").asBoolean(false))
            throw new IllegalStateException("Zalo " + operation + " failed: " + response);
    }
}
