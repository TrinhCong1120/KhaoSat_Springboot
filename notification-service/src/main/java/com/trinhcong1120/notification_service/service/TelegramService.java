package com.trinhcong1120.notification_service.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
public class TelegramService {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private final String token;
    private final String webhookUrl;
    private final String webhookSecret;
    private final RestClient client;
    private final JdbcTemplate jdbc;
    private InboxStore inbox;

    @Autowired(required = false)
    public void setInbox(InboxStore inbox) { this.inbox = inbox; }

    @Autowired
    public TelegramService(@Value("${spring.telegram.bot.token:}") String token,
                           @Value("${spring.telegram.bot.webhook-url:}") String webhookUrl,
                           @Value("${spring.telegram.bot.webhook-secret:}") String webhookSecret,
                           JdbcTemplate jdbc) {
        this(token, webhookUrl, webhookSecret, jdbc, createClient(token));
    }

    TelegramService(String token, String webhookUrl, String webhookSecret,
                    JdbcTemplate jdbc, RestClient client) {
        this.token = token == null ? "" : token.trim();
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.jdbc = jdbc;
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
                    Map.of("command", "start", "description", "Mở menu"),
                    Map.of("command", "thongtin", "description", "Kiểm tra tài khoản liên kết"),
                    Map.of("command", "dangky", "description", "Hướng dẫn đăng ký liên kết"),
                    Map.of("command", "getid", "description", "Lấy Chat ID cho quản trị viên"))))
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
        String command = raw.isBlank() ? "" : raw.split("\\s+", 2)[0].split("@", 2)[0].toLowerCase();
        String action = switch (command) {
            case "/start", "start" -> "START";
            case "/thongtin", "info" -> "INFO";
            case "/dangky", "register" -> "REGISTER";
            case "/getid", "getid" -> "GET_ID";
            default -> null;
        };
        if (!isCallback && inbox != null) {
            String senderAddress = Long.toString(chatId);
            String replyId = message.path("reply_to_message").path("message_id").isMissingNode()
                    ? null : message.path("reply_to_message").path("message_id").asText();
            UUID inboundId = inbox.save("TELEGRAM", "telegram:" + updateId,
                    message.path("message_id").asText(), senderAddress, "bot",
                    null, raw.isBlank() ? message.path("caption").asText("") : raw,
                    action == null ? "MESSAGE" : "COMMAND",
                    Instant.ofEpochSecond(message.path("date").asLong(Instant.now().getEpochSecond())), replyId);
            saveFile(message, inboundId);
        }
        if (action == null) return;

        List<Account> accounts = jdbc.query("SELECT id, username, email FROM users WHERE telegram_chat_id = ?",
                (rs, n) -> new Account(rs.getObject("id", UUID.class), rs.getString("username"), rs.getString("email")), chatId);
        Account account = accounts.isEmpty() ? null : accounts.get(0);
        int inserted = jdbc.update("""
                INSERT INTO telegram_chat_messages (update_id, chat_id, account_user_id, direction, action, content, status)
                VALUES (?, ?, ?, 'IN', ?, ?, 'RECEIVED') ON CONFLICT (update_id, direction) DO NOTHING
                """, updateId, chatId, account == null ? null : account.id(), action,
                isCallback ? "Nút: " + action : command);
        if (inserted == 0) {
            List<String> replies = jdbc.query(
                    "SELECT status FROM telegram_chat_messages WHERE update_id = ? AND direction = 'OUT'",
                    (rs, n) -> rs.getString(1), updateId);
            if (!replies.isEmpty() && "SENT".equals(replies.get(0))) return;
        }
        if (isCallback) {
            try {
                ensureOk(client.post().uri("/answerCallbackQuery")
                        .body(Map.of("callback_query_id", callback.path("id").asText()))
                        .retrieve().body(JsonNode.class), "answerCallbackQuery");
            } catch (RuntimeException ex) {
                log.warn("Could not acknowledge Telegram callback {}", updateId, ex);
            }
        }

        String response = switch (action) {
            case "START" -> "Chào bạn! Chọn một mục bên dưới hoặc dùng /thongtin, /dangky, /getid.";
            case "INFO" -> account == null
                    ? "Chat này chưa liên kết với tài khoản hệ thống. Chọn Đăng ký thông tin để biết cách liên kết."
                    : "Tài khoản đã liên kết: " + account.username() + "\nEmail: " + maskEmail(account.email());
            case "REGISTER" -> "Để liên kết tài khoản, gửi Chat ID " + chatId
                    + " cho quản trị viên. Quản trị viên sẽ nhập ID vào tài khoản của bạn trong hệ thống."
                    + "\nKhông gửi mật khẩu hoặc mã đăng nhập qua Telegram.";
            default -> "Chat ID của bạn: " + chatId + "\nGửi ID này cho quản trị viên để liên kết tài khoản.";
        };
        try {
            sendMessage(chatId, response, "START".equals(action));
            logReply(updateId, chatId, account, action, response, "SENT", null);
        } catch (RuntimeException ex) {
            logReply(updateId, chatId, account, action, response, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    private void logReply(long updateId, long chatId, Account account, String action,
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
                        List.of(Map.of("text", "🔎 Kiểm tra thông tin", "callback_data", "info")),
                        List.of(Map.of("text", "📝 Đăng ký thông tin", "callback_data", "register")))))
                : Map.of("chat_id", chatId, "text", text);
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

    private record Account(UUID id, String username, String email) {}
}
