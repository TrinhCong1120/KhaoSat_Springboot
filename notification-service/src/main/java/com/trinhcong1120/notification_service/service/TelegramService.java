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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
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
    public TelegramService(@Value("${spring.telegram.bot.token:}") String token,
                           @Value("${spring.telegram.bot.webhook-url:}") String webhookUrl,
                           @Value("${spring.telegram.bot.webhook-secret:}") String webhookSecret,
                           JdbcTemplate jdbc,
                           @Autowired(required = false) BotInteractionService botService) {
        this(token, webhookUrl, webhookSecret, jdbc,
                botService != null ? botService : new BotInteractionService(jdbc),
                createClient(token));
    }

    TelegramService(String token, String webhookUrl, String webhookSecret,
                    JdbcTemplate jdbc, RestClient client) {
        this(token, webhookUrl, webhookSecret, jdbc, new BotInteractionService(jdbc), client);
    }

    TelegramService(String token, String webhookUrl, String webhookSecret,
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
        return RestClient.builder().baseUrl("https://api.telegram.org/bot" + (token == null ? "" : token.trim()))
                .requestFactory(requests).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        if (token.isBlank() || webhookUrl.isBlank() || webhookSecret.isBlank()) {
            log.warn("Telegram webhook disabled: configure TELEGRAM_BOT_TOKEN, TELEGRAM_WEBHOOK_URL and TELEGRAM_WEBHOOK_SECRET");
            return;
        }
        if (!webhookUrl.startsWith("https://") || !webhookSecret.matches("[A-Za-z0-9_-]{1,256}"))
            throw new IllegalStateException("Invalid Telegram webhook URL or secret");
        try {
            ensureOk(client.post().uri("/setWebhook")
                    .body(Map.of("url", webhookUrl, "secret_token", webhookSecret,
                            "allowed_updates", List.of("message", "callback_query"),
                            "max_connections", 1, "drop_pending_updates", false))
                    .retrieve().body(JsonNode.class), "setWebhook");

            ensureOk(client.post().uri("/setMyCommands").body(Map.of("commands", List.of(
                    Map.of("command", "start", "description", "Mở Menu chính"),
                    Map.of("command", "thongke", "description", "Báo cáo thống kê khảo sát"),
                    Map.of("command", "khaosat", "description", "Danh sách khảo sát của tôi"),
                    Map.of("command", "thongtin", "description", "Thông tin tài khoản"),
                    Map.of("command", "link", "description", "Liên kết tài khoản: /link <mã>"),
                    Map.of("command", "getid", "description", "Lấy Chat ID cá nhân"),
                    Map.of("command", "help", "description", "Hướng dẫn sử dụng"))))
                    .retrieve().body(JsonNode.class), "setMyCommands");
        } catch (RuntimeException ex) {
            log.error("Could not register Telegram webhook or commands", ex);
        }
    }

    public String webhookSecret() { return webhookSecret; }

    public void handleUpdate(JsonNode update) {
        long updateId = update.path("update_id").asLong(-1);
        JsonNode callback = update.path("callback_query");
        boolean isCallback = !callback.isMissingNode() && !callback.isNull();
        JsonNode message = isCallback ? callback.path("message") : update.path("message");
        JsonNode chat = message.path("chat");
        if (updateId < 0 || !"private".equals(chat.path("type").asText())) return;
        long chatId = chat.path("id").asLong();
        JsonNode sender = isCallback ? callback.path("from") : message.path("from");
        if (sender.path("id").asLong() != chatId) return;

        String raw = isCallback ? callback.path("data").asText("") : message.path("text").asText("").trim();
        String textTrimmed = raw.trim();
        String command = textTrimmed.isBlank() ? "" : textTrimmed.split("\\s+", 2)[0].split("@", 2)[0].toLowerCase();
        String argument = textTrimmed.contains(" ") ? textTrimmed.substring(textTrimmed.indexOf(' ') + 1).trim() : "";

        // Kiểm tra tránh xử lý trùng lặp update
        try {
            List<String> existing = jdbc.query(
                    "SELECT status FROM telegram_chat_messages WHERE update_id = ? AND direction = 'OUT'",
                    (rs, rowNum) -> rs.getString("status"), updateId);
            if (!existing.isEmpty() && "SENT".equals(existing.get(0))) {
                log.info("Duplicate Telegram update {} already processed, skipping", updateId);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to check duplicate update for chat {}", chatId, ex);
        }

        // Lưu vào inbox nếu là tin nhắn thường
        if (!isCallback && inbox != null) {
            String senderAddress = Long.toString(chatId);
            String replyId = message.path("reply_to_message").path("message_id").isMissingNode()
                    ? null : message.path("reply_to_message").path("message_id").asText();
            UUID inboundId = inbox.save("TELEGRAM", "telegram:" + updateId,
                    message.path("message_id").asText(), senderAddress, "bot",
                    null, raw.isBlank() ? message.path("caption").asText("") : raw,
                    command.startsWith("/") ? "COMMAND" : "MESSAGE",
                    Instant.ofEpochSecond(message.path("date").asLong(Instant.now().getEpochSecond())), replyId);
            saveFile(message, inboundId);
        }

        // Lấy thông tin tài khoản liên kết (kèm quyền Admin)
        BotInteractionService.BotAccount account = botService.getAccountByChatId("TELEGRAM", String.valueOf(chatId));

        if (isCallback) {
            try {
                ensureOk(client.post().uri("/answerCallbackQuery")
                        .body(Map.of("callback_query_id", callback.path("id").asText()))
                        .retrieve().body(JsonNode.class), "answerCallbackQuery");
            } catch (RuntimeException ex) {
                log.warn("Could not acknowledge Telegram callback {}", updateId, ex);
            }
        }

        // Xử lý deep-linking: /start link_123456
        if (command.equals("/start") && argument.startsWith("link_")) {
            String otp = argument.substring("link_".length()).trim();
            command = "/link";
            argument = otp;
        }

        // Action routing
        BotResponse botResponse;
        try {
            botResponse = processAction(command, argument, isCallback, chatId, account);
        } catch (Exception ex) {
            log.error("Error processing Telegram action {} for chat {}", command, chatId, ex);
            botResponse = new BotResponse("ERROR",
                    "⚠️ Đã có lỗi xảy ra khi xử lý dữ liệu. Vui lòng thử lại sau giây lát.",
                    List.of(List.of(btn("🏠 Menu chính", "start"))));
        }

        // Lưu tin nhắn đến vào bảng chat
        try {
            jdbc.update("""
                    INSERT INTO telegram_chat_messages (update_id, chat_id, account_user_id, direction, action, content, status)
                    VALUES (?, ?, ?, 'IN', ?, ?, 'RECEIVED') ON CONFLICT (update_id, direction) DO NOTHING
                    """, updateId, chatId, account == null ? null : account.id(),
                    botResponse.action(), isCallback ? "Nút: " + raw : raw);
        } catch (Exception ex) {
            log.warn("Failed to record incoming telegram message for chat {}", chatId, ex);
        }

        // Gửi phản hồi (hỗ trợ gửi tin nhắn phụ nối tiếp trước tin chính nếu có)
        try {
            if (botResponse.extraMessages() != null && !botResponse.extraMessages().isEmpty()) {
                for (String extra : botResponse.extraMessages()) {
                    sendMessage(chatId, extra, false);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (botResponse.keyboard() != null && !botResponse.keyboard().isEmpty()) {
                sendKeyboard(chatId, botResponse.text(), botResponse.keyboard());
            } else {
                sendMessage(chatId, botResponse.text(), false);
            }
            String loggedContent = (botResponse.extraMessages() == null || botResponse.extraMessages().isEmpty())
                    ? botResponse.text()
                    : String.join("\n\n---\n\n", botResponse.extraMessages()) + "\n\n---\n\n" + botResponse.text();
            logReply(updateId, chatId, account, botResponse.action(), loggedContent, "SENT", null);
        } catch (RuntimeException ex) {
            logReply(updateId, chatId, account, botResponse.action(), botResponse.text(), "FAILED", ex.getMessage());
            log.error("Failed to send telegram reply to chat {}", chatId, ex);
        }
    }

    private record BotResponse(String action, String text, List<List<Map<String, String>>> keyboard, List<String> extraMessages) {
        BotResponse(String action, String text, List<List<Map<String, String>>> keyboard) {
            this(action, text, keyboard, List.of());
        }
    }

    private BotResponse processAction(String command, String argument, boolean isCallback, long chatId,
                                      BotInteractionService.BotAccount account) {
        // Phân giải callback query dạng tiền tố
        if (isCallback) {
            if (command.startsWith("surveys_")) {
                int page = 0;
                try { page = Integer.parseInt(command.substring("surveys_".length())); } catch (Exception ignored) {}
                return handleSurveys(account, page);
            }
            if (command.startsWith("survey_")) {
                String idStr = command.substring("survey_".length());
                return handleSurveyDetail(account, idStr);
            }
        }

        return switch (command) {
            case "/start", "start" -> handleStart(account, chatId);
            case "/thongke", "/stats", "stats" -> handleStats(account);
            case "/khaosat", "/surveys", "my_surveys" -> handleSurveys(account, 0);
            case "/hethong", "/admin", "admin_sys" -> handleAdminSystem(account);
            case "/canhbao", "admin_fail" -> handleAdminFailedCases(account);
            case "/thongtin", "/info", "info" -> handleInfo(account, chatId);
            case "/getid", "getid" -> handleGetId(chatId);
            case "/dangky", "/register", "register", "link_guide" -> handleRegisterGuide(account, chatId);
            case "/link", "/lienket" -> handleLinkCode(account, argument, chatId);
            case "/unlink", "/huylienket", "/huy", "unlink" -> handleUnlink(chatId);
            case "/help", "/trogiup", "/huongdan", "help" -> handleHelp(account);
            default -> {
                // Kiểm tra nếu gõ /ks_<uuid>
                if (command.startsWith("/ks_")) {
                    yield handleSurveyDetail(account, command.substring(4));
                }
                yield handleStart(account, chatId);
            }
        };
    }

    private BotResponse handleStart(BotInteractionService.BotAccount account, long chatId) {
        if (account == null) {
            String welcomeMsg = """
                    👋 CHÀO MỪNG BẠN ĐẾN VỚI BOT KHẢO SÁT & BÁO CÁO TỰ ĐỘNG!
                    ─────────────────────────
                    🤖 Bot sẽ đồng hành cùng bạn để:
                    📊 Theo dõi số liệu thống kê & phản hồi theo thời gian thực
                    📋 Tra cứu danh sách & xem chi tiết các bài khảo sát
                    🔔 Nhận thông báo tức thì ngay khi có người gửi bài khảo sát
                    🛠 Quản trị viên: Giám sát toàn hệ thống & cảnh báo bài nộp lỗi""";

            String guideMsg = """
                    🚀 HƯỚNG DẪN BẮT ĐẦU SỬ DỤNG:
                    ─────────────────────────
                    Tài khoản Telegram này chưa được liên kết với hệ thống khảo sát trên Web.
                    Hãy thực hiện các bước sau để kết nối:

                    1️⃣ Đăng nhập vào Website Khảo sát.
                    2️⃣ Bấm vào nút 🤖 "Bot thông báo" trên thanh Header.
                    3️⃣ Chọn Telegram để lấy Mã OTP 6 chữ số (hiệu lực 10 phút).
                    4️⃣ Gửi mã cho Bot theo cú pháp:
                       👉 /link <mã_OTP>   (Ví dụ: /link 123456)
                       (Hoặc bấm nút "Mở Telegram liên kết tự động" trên Web)

                    📖 CẨM NANG CÁC CÂU LỆNH:
                    • /link : Xem hướng dẫn liên kết chi tiết
                    • /thongke : Báo cáo thống kê khảo sát của bạn
                    • /khaosat : Danh sách bài khảo sát (kèm nút lật trang)
                    • /thongtin : Kiểm tra tài khoản đang liên kết
                    • /getid : Lấy Chat ID của bạn để hỗ trợ thủ công
                    • /help : Mở hướng dẫn sử dụng đầy đủ""";

            List<List<Map<String, String>>> kb = List.of(
                    List.of(btn("🔗 Hướng dẫn liên kết (/link)", "link_guide")),
                    List.of(btn("❓ Hướng dẫn sử dụng (/help)", "help"), btn("🆔 Lấy Chat ID", "getid"))
            );
            return new BotResponse("START", guideMsg, kb, List.of(welcomeMsg));
        }

        String text = String.format("""
                👋 Xin chào, %s!
                
                Chào mừng bạn quay lại hệ thống khảo sát. Hãy chọn một mục bên dưới để xem báo cáo hoặc quản lý khảo sát của bạn:""",
                account.username());

        List<List<Map<String, String>>> kb = new ArrayList<>();
        kb.add(List.of(
                btn("📊 Thống kê của tôi", "stats"),
                btn("📋 Khảo sát của tôi", "my_surveys")
        ));
        kb.add(List.of(
                btn("👤 Tài khoản", "info"),
                btn("❓ Trợ giúp", "help")
        ));

        if (account.isAdmin()) {
            kb.add(List.of(
                    btn("🛠 Báo cáo hệ thống", "admin_sys"),
                    btn("⚠️ Khảo sát lỗi", "admin_fail")
            ));
        }

        return new BotResponse("START", text, kb);
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
                Bấm "📋 Xem khảo sát" để xem danh sách chi tiết.""",
                account.username(), stats.totalSurveys(), stats.activeSurveys(),
                stats.totalResponses(), stats.todayResponses());

        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("📋 Xem khảo sát", "my_surveys"), btn("🔄 Làm mới", "stats")),
                List.of(btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("STATS", text, kb);
    }

    private BotResponse handleSurveys(BotInteractionService.BotAccount account, int page) {
        if (account == null) return requireLinkResponse();

        int pageSize = 5;
        int total = botService.countUserSurveys(account.id(), account.isAdmin());
        List<BotInteractionService.SurveySummaryDto> surveys = botService.getUserSurveys(account.id(), account.isAdmin(), page, pageSize);

        if (surveys.isEmpty()) {
            String text = "📋 Bạn chưa có bài khảo sát nào được tạo hoặc được phân quyền truy cập.";
            List<List<Map<String, String>>> kb = List.of(List.of(btn("🏠 Menu chính", "start")));
            return new BotResponse("SURVEYS", text, kb);
        }

        int maxPage = (int) Math.ceil((double) total / pageSize) - 1;
        StringBuilder sb = new StringBuilder();
        sb.append("📋 DANH SÁCH KHẢO SÁT\n");
        sb.append(String.format("Trang %d/%d • Tổng số %d bài khảo sát\n", page + 1, Math.max(1, maxPage + 1), total));
        sb.append("═════════════════════════\n\n");

        List<List<Map<String, String>>> kb = new ArrayList<>();
        int idx = page * pageSize + 1;
        for (BotInteractionService.SurveySummaryDto s : surveys) {
            String status = s.isActive() ? "🟢 Đang mở" : "🔴 Đang đóng";
            sb.append(String.format("🔹 [%d] %s\n", idx, s.title()));
            sb.append(String.format("     %s • 📄 %d trang | ❓ %d câu | 💬 %d phản hồi\n\n",
                    status, s.pageCount(), s.questionCount(), s.responseCount()));

            // Nút xem chi tiết từng khảo sát
            String btnLabel = "🔍 [" + idx + "] " + (s.title().length() > 24 ? s.title().substring(0, 22) + "..." : s.title());
            kb.add(List.of(btn(btnLabel, "survey_" + s.id())));
            idx++;
        }

        sb.append("═════════════════════════\n");
        sb.append("👇 Bấm vào nút bên dưới để xem chi tiết & nhận link làm khảo sát:");

        // Hàng nút phân trang
        List<Map<String, String>> navRow = new ArrayList<>();
        if (page > 0) {
            navRow.add(btn("⬅️ Trang trước", "surveys_" + (page - 1)));
        }
        if (page < maxPage) {
            navRow.add(btn("Trang sau ➡️", "surveys_" + (page + 1)));
        }
        if (!navRow.isEmpty()) {
            kb.add(navRow);
        }
        kb.add(List.of(btn("📊 Xem thống kê", "stats"), btn("🏠 Menu chính", "start")));

        return new BotResponse("SURVEYS", sb.toString(), kb);
    }

    private BotResponse handleSurveyDetail(BotInteractionService.BotAccount account, String idStr) {
        if (account == null) return requireLinkResponse();

        BotInteractionService.SurveyDetailDto d = botService.getSurveyByCodeOrId(account.id(), idStr, account.isAdmin());
        if (d == null) {
            return new BotResponse("SURVEY_DETAIL", "❌ Không tìm thấy khảo sát hoặc bạn không có quyền xem khảo sát này.",
                    List.of(List.of(btn("🔙 Danh sách khảo sát", "my_surveys"))));
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
                (Sao chép đường dẫn trên để gửi cho người tham gia)
                ═════════════════════════""",
                d.title(), status, desc, d.pageCount(), d.questionCount(), d.responseCount(), createdDate, shareRef);

        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("🔙 Quay lại danh sách", "my_surveys")),
                List.of(btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("SURVEY_DETAIL", text, kb);
    }

    private BotResponse handleAdminSystem(BotInteractionService.BotAccount account) {
        if (account == null || !account.isAdmin()) {
            return new BotResponse("ADMIN", "⛔ Bạn không có quyền truy cập vào báo cáo quản trị hệ thống.",
                    List.of(List.of(btn("🏠 Menu chính", "start"))));
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
                ─────────────────────────""",
                stats.totalUsers(), stats.totalSurveys(), stats.activeSurveys(),
                stats.totalResponses(), stats.todayResponses(), stats.pendingFailedCases());

        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("⚠️ Xem khảo sát lỗi", "admin_fail"), btn("🔄 Làm mới", "admin_sys")),
                List.of(btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("ADMIN_STATS", text, kb);
    }

    private BotResponse handleAdminFailedCases(BotInteractionService.BotAccount account) {
        if (account == null || !account.isAdmin()) {
            return new BotResponse("ADMIN", "⛔ Bạn không có quyền truy cập chức năng này.",
                    List.of(List.of(btn("🏠 Menu chính", "start"))));
        }

        List<BotInteractionService.FailedCaseSummaryDto> cases = botService.getRecentFailedCases(5);
        if (cases.isEmpty()) {
            String text = "✅ Tuyệt vời! Hiện tại không có bài khảo sát nào bị lỗi gửi.";
            List<List<Map<String, String>>> kb = List.of(List.of(btn("🏠 Menu chính", "start")));
            return new BotResponse("ADMIN_FAIL", text, kb);
        }

        StringBuilder sb = new StringBuilder("⚠️ DANH SÁCH BÀI NỘP KHẢO SÁT LỖI (Gần đây):\n─────────────────────────\n");
        for (BotInteractionService.FailedCaseSummaryDto c : cases) {
            String time = c.createdAt() != null ? DATE_TIME_FORMATTER.format(c.createdAt()) : "N/A";
            sb.append(String.format("• Tệp: %s\n  Trạng thái: %s | Cỡ: %d bytes\n  Thời gian: %s\n\n",
                    c.originalFileName(), c.status(), c.fileSize(), time));
        }

        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("🔄 Làm mới", "admin_fail"), btn("🛠 Báo cáo hệ thống", "admin_sys")),
                List.of(btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("ADMIN_FAIL", sb.toString(), kb);
    }

    private BotResponse handleInfo(BotInteractionService.BotAccount account, long chatId) {
        if (account == null) {
            String text = "Chat này chưa liên kết với tài khoản hệ thống.\nChat ID của bạn: " + chatId;
            return new BotResponse("INFO", text, List.of(List.of(btn("🔗 Liên kết ngay", "link_guide"))));
        }

        String role = account.isAdmin() ? "Quản trị viên (Admin/Manager)" : "Người dùng / Khảo sát viên";
        String text = String.format("""
                👤 THÔNG TIN TÀI KHOẢN LIÊN KẾT
                ─────────────────────────
                Tên đăng nhập: %s
                Email: %s
                Vai trò: %s
                Telegram Chat ID: %d
                ─────────────────────────
                Để hủy liên kết chat này, gửi lệnh /huylienket""",
                account.username(), maskEmail(account.email()), role, chatId);

        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("📊 Thống kê của tôi", "stats"), btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("INFO", text, kb);
    }

    private BotResponse handleLinkCode(BotInteractionService.BotAccount account, String code, long chatId) {
        if (account != null && (code == null || code.isBlank())) {
            String role = account.isAdmin() ? "Quản trị viên (Admin/Manager)" : "Người dùng / Khảo sát viên";
            String text = String.format("""
                    ℹ️ BẠN ĐÃ LIÊN KẾT TÀI KHOẢN RỒI!
                    ─────────────────────────
                    👤 Tên tài khoản: %s
                    📧 Email: %s
                    🔑 Vai trò: %s
                    ─────────────────────────
                    Bạn đã có thể sử dụng đầy đủ các tính năng:
                    • /thongke : Xem số liệu thống kê cá nhân
                    • /khaosat : Xem danh sách khảo sát của bạn
                    
                    Nếu bạn muốn đổi sang tài khoản khác, hãy gửi: /huylienket rồi liên kết lại.""",
                    account.username(), maskEmail(account.email()), role);
            List<List<Map<String, String>>> kb = List.of(
                    List.of(btn("📊 Xem thống kê của tôi", "stats"), btn("📋 Xem khảo sát của tôi", "my_surveys")),
                    List.of(btn("🏠 Menu chính", "start"))
            );
            return new BotResponse("ALREADY_LINKED", text, kb);
        }

        if (code == null || code.isBlank()) {
            return handleRegisterGuide(account, chatId);
        }

        String username = botService.linkAccountByCode(code.trim(), "TELEGRAM", String.valueOf(chatId));
        if (username != null) {
            String text = String.format("""
                    🎉 CHÚC MỪNG BẠN ĐÃ LIÊN KẾT THÀNH CÔNG!
                    ─────────────────────────
                    Xin chào %s! Tài khoản Telegram đã được kết nối với hệ thống khảo sát.

                    🚀 HƯỚNG DẪN BẮT ĐẦU SỬ DỤNG:
                    1️⃣ /thongke (hoặc bấm "📊 Thống kê"): Xem báo cáo tổng phản hồi & tỷ lệ hoàn thành.
                    2️⃣ /khaosat (hoặc bấm "📋 Khảo sát"): Xem danh sách khảo sát, lật trang & xem chi tiết.
                    3️⃣ Nhận thông báo tự động: Khi có người nộp bài khảo sát, Bot sẽ gửi tin nhắn báo ngay!
                    4️⃣ /help : Xem cẩm nang toàn bộ câu lệnh bất cứ lúc nào.
                    ─────────────────────────
                    Bấm vào các nút bên dưới để bắt đầu:""", username);
            List<List<Map<String, String>>> kb = List.of(
                    List.of(btn("📊 Xem thống kê của tôi", "stats"), btn("📋 Khảo sát của tôi", "my_surveys")),
                    List.of(btn("❓ Hướng dẫn sử dụng", "help"), btn("🏠 Menu chính", "start"))
            );
            return new BotResponse("LINK_SUCCESS", text, kb);
        } else {
            String text = """
                    ❌ MÃ LIÊN KẾT KHÔNG CHÍNH XÁC HOẶC ĐÃ HẾT HẠN!
                    ─────────────────────────
                    Mã liên kết chỉ có hiệu lực trong 10 phút và chỉ sử dụng được 1 lần duy nhất.

                    👉 Cách lấy mã mới:
                    1. Đăng nhập Website Khảo sát.
                    2. Bấm nút "Bot thông báo" trên thanh Header.
                    3. Nhấn "Tạo mã mới" và nhập lại: /link <mã_mới>
                    
                    Gõ /link để xem lại hướng dẫn chi tiết.""";
            List<List<Map<String, String>>> kb = List.of(
                    List.of(btn("🔗 Hướng dẫn liên kết (/link)", "link_guide")),
                    List.of(btn("🏠 Menu chính", "start"))
            );
            return new BotResponse("LINK_FAILED", text, kb);
        }
    }

    private BotResponse handleUnlink(long chatId) {
        boolean unlinked = botService.unlinkAccount("TELEGRAM", String.valueOf(chatId));
        String text = unlinked
                ? "✅ Đã hủy liên kết tài khoản khỏi chat này thành công.\nBạn có thể gửi /link bất cứ lúc nào để liên kết lại."
                : "Tài khoản hiện chưa được liên kết.";
        return new BotResponse("UNLINK", text, List.of(List.of(btn("🔗 Hướng dẫn liên kết lại", "link_guide"))));
    }

    private BotResponse handleGetId(long chatId) {
        String text = String.format("""
                🆔 Chat ID của bạn là: %d
                
                Nếu không dùng mã kết nối tự động, bạn có thể gửi Chat ID này cho quản trị viên để gán thủ công vào tài khoản.""", chatId);
        return new BotResponse("GET_ID", text, List.of(List.of(btn("🏠 Menu chính", "start"))));
    }

    private BotResponse handleRegisterGuide(BotInteractionService.BotAccount account, long chatId) {
        String text = String.format("""
                🔗 HƯỚNG DẪN LIÊN KẾT TÀI KHOẢN TỪ WEB QUA BOT
                ─────────────────────────
                Để Bot có thể gửi thông báo và hiển thị dữ liệu khảo sát của bạn, hãy thực hiện theo 4 bước sau:

                1️⃣ Bước 1: Mở trình duyệt và đăng nhập vào Website Khảo sát của bạn.
                
                2️⃣ Bước 2: Nhìn lên góc trên bên phải thanh Header, bấm vào nút:
                   🤖 "Bot thông báo"
                
                3️⃣ Bước 3: Cửa sổ liên kết hiện ra, chọn tab "Telegram Bot".
                   Hệ thống sẽ cấp cho bạn một Mã OTP gồm 6 chữ số (Ví dụ: 824915, có hiệu lực trong 10 phút).
                
                4️⃣ Bước 4: Quay lại khung chat Telegram này và gửi tin nhắn theo cú pháp:
                   👉 /link <mã_OTP>
                   (Ví dụ: /link 824915)

                💡 Mẹo: Trên cửa sổ Web có nút "Mở Telegram liên kết tự động", bạn bấm vào là Bot sẽ tự động kích hoạt ngay mà không cần gõ mã!
                ─────────────────────────
                Gặp khó khăn? Chat ID của bạn là: %d
                Bạn có thể gửi Chat ID này cho Quản trị viên để được hỗ trợ thủ công.""", chatId);

        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("📖 Xem danh sách lệnh (/help)", "help")),
                List.of(btn("🆔 Lấy Chat ID của tôi (/getid)", "getid")),
                List.of(btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("LINK_GUIDE", text, kb);
    }

    private BotResponse handleHelp(BotInteractionService.BotAccount account) {
        StringBuilder sb = new StringBuilder();
        sb.append("📖 CẨM NANG HƯỚNG DẪN SỬ DỤNG BOT KHẢO SÁT\n");
        sb.append("─────────────────────────\n\n");
        sb.append("🔹 LỆNH CƠ BẢN DÀNH CHO MỌI NGƯỜI DÙNG:\n");
        sb.append("• /start : Mở Menu chính và các nút bấm tương tác\n");
        sb.append("• /link : Hướng dẫn cách lấy mã kết nối từ Web qua Bot\n");
        sb.append("• /link <mã> : Kích hoạt liên kết tài khoản bằng mã OTP 6 số\n");
        sb.append("• /thongke : Báo cáo thống kê khảo sát và phản hồi của bạn\n");
        sb.append("• /khaosat : Danh sách khảo sát được phân quyền (kèm nút lật trang)\n");
        sb.append("• /ks_<id> : Xem chi tiết một bài khảo sát (thời lượng, link làm bài...)\n");
        sb.append("• /thongtin : Kiểm tra tài khoản Web đang liên kết\n");
        sb.append("• /huylienket : Hủy liên kết tài khoản khỏi chat này\n");
        sb.append("• /getid : Lấy Chat ID của bạn (hỗ trợ liên kết thủ công)\n");
        sb.append("• /help : Xem danh sách hướng dẫn này\n");

        if (account != null && account.isAdmin()) {
            sb.append("\n🛠 DÀNH CHO QUẢN TRỊ VIÊN (ADMIN/MANAGER):\n");
            sb.append("• /hethong : Báo cáo thống kê toàn bộ hệ thống\n");
            sb.append("• /canhbao : Xem danh sách các bài khảo sát nộp bị lỗi\n");
        }

        sb.append("\n💡 Mẹo: Bạn có thể sử dụng các nút bấm (Inline Keyboard) bên dưới tin nhắn để thao tác nhanh mà không cần gõ lệnh!");
        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("📊 Thống kê", "stats"), btn("📋 Danh sách khảo sát", "my_surveys")),
                List.of(btn("🔗 Hướng dẫn liên kết", "link_guide"), btn("🏠 Menu chính", "start"))
        );
        return new BotResponse("HELP", sb.toString(), kb);
    }

    private BotResponse requireLinkResponse() {
        String text = """
                ⚠️ BẠN CHƯA LIÊN KẾT TÀI KHOẢN!
                ─────────────────────────
                Để xem số liệu thống kê và danh sách bài khảo sát của bạn, hãy liên kết tài khoản Web với Bot.
                
                👉 Gửi lệnh: /link để xem hướng dẫn 4 bước liên kết cực nhanh!
                👉 Nếu đã có mã OTP từ Web, hãy gửi ngay: /link <mã_OTP>""";
        List<List<Map<String, String>>> kb = List.of(
                List.of(btn("🔗 Hướng dẫn liên kết (/link)", "link_guide")),
                List.of(btn("❓ Hướng dẫn sử dụng (/help)", "help"))
        );
        return new BotResponse("REQUIRE_LINK", text, kb);
    }

    private static Map<String, String> btn(String text, String callbackData) {
        return Map.of("text", text, "callback_data", callbackData);
    }

    private void logReply(long updateId, long chatId, BotInteractionService.BotAccount account, String action,
                          String content, String status, String error) {
        jdbc.update("""
                INSERT INTO telegram_chat_messages
                (update_id, chat_id, account_user_id, direction, action, content, status, error_note)
                VALUES (?, ?, ?, 'OUT', ?, ?, ?, ?)
                ON CONFLICT (update_id, direction) DO UPDATE SET status = EXCLUDED.status, error_note = EXCLUDED.error_note
                """, updateId, chatId, account == null ? null : account.id(), action, content,
                status, error == null ? null : error.substring(0, Math.min(error.length(), 500)));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "chưa có";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(1, at)) + "***" + email.substring(at);
    }

    public String sendNotification(Long chatId, String subject, String message) {
        return sendNotification(chatId, subject, message, null);
    }

    public String sendNotification(Long chatId, String subject, String message, String replyToId) {
        if (token.isBlank()) throw new IllegalStateException("Telegram bot token is not configured");
        if (replyToId == null) return sendMessage(chatId, subject + "\n\n" + message, false);
        long reply = Long.parseLong(replyToId);
        JsonNode response = client.post().uri("/sendMessage")
                .body(Map.of("chat_id", chatId, "text", subject + "\n\n" + message,
                        "reply_parameters", Map.of("message_id", reply, "allow_sending_without_reply", true)))
                .retrieve().body(JsonNode.class);
        ensureOk(response, "sendMessage");
        return response.path("result").path("message_id").isMissingNode() ? null
                : response.path("result").path("message_id").asText();
    }

    private String sendMessage(long chatId, String text, boolean menu) {
        Map<String, Object> body = menu ? Map.of("chat_id", chatId, "text", text, "reply_markup",
                Map.of("inline_keyboard", List.of(
                        List.of(btn("📊 Thống kê của tôi", "stats"), btn("📋 Khảo sát của tôi", "my_surveys")),
                        List.of(btn("👤 Tài khoản", "info"), btn("❓ Trợ giúp", "help")))))
                : Map.of("chat_id", chatId, "text", text);
        JsonNode response = client.post().uri("/sendMessage").body(body).retrieve().body(JsonNode.class);
        ensureOk(response, "sendMessage");
        return response.path("result").path("message_id").isMissingNode() ? null
                : response.path("result").path("message_id").asText();
    }

    private String sendKeyboard(long chatId, String text, List<List<Map<String, String>>> keyboard) {
        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text,
                "reply_markup", Map.of("inline_keyboard", keyboard)
        );
        JsonNode response = client.post().uri("/sendMessage").body(body).retrieve().body(JsonNode.class);
        ensureOk(response, "sendMessage");
        return response.path("result").path("message_id").isMissingNode() ? null
                : response.path("result").path("message_id").asText();
    }

    private void saveFile(JsonNode message, UUID inboundId) {
        JsonNode file = message.path("document");
        if (file.isMissingNode()) file = message.path("photo");
        if (file.isMissingNode()) file = message.path("audio");
        if (file.isMissingNode()) file = message.path("video");
        if (file.isMissingNode()) file = message.path("voice");
        if (file.isMissingNode()) file = message.path("video_note");
        if (file.isMissingNode()) file = message.path("animation");
        if (file.isMissingNode()) file = message.path("sticker");
        if (file.isArray() && file.size() > 0) file = file.get(file.size() - 1);
        if (file.isMissingNode() || file.isNull()) return;
        String fileId = file.path("file_id").asText("");
        if (fileId.isBlank()) return;
        String name = file.path("file_name").asText("telegram-" + fileId);
        String type = file.path("mime_type").asText("application/octet-stream");
        long size = file.path("file_size").asLong(-1);
        if (size > inbox.maxBytes()) {
            inbox.attachment(inboundId, fileId, name, type, size, InputStream.nullInputStream());
            return;
        }
        try {
            JsonNode result = client.post().uri("/getFile").body(Map.of("file_id", fileId))
                    .retrieve().body(JsonNode.class);
            ensureOk(result, "getFile");
            String path = result.path("result").path("file_path").asText();
            byte[] bytes = RestClient.create().get()
                    .uri("https://api.telegram.org/file/bot" + token + "/" + path)
                    .retrieve().body(byte[].class);
            if (bytes == null) throw new IllegalStateException("Empty Telegram file");
            inbox.attachment(inboundId, fileId, name, type, bytes.length, new ByteArrayInputStream(bytes));
        } catch (RuntimeException ex) {
            log.warn("Could not download Telegram attachment for update", ex);
            inbox.failedAttachment(inboundId, fileId, name, type, size,
                    "Không tải được tệp từ Telegram: " + ex.getClass().getSimpleName());
        }
    }

    private void ensureOk(JsonNode response, String operation) {
        if (response == null || !response.path("ok").asBoolean(false))
            throw new IllegalStateException("Telegram " + operation + " failed: " + response);
    }
}
