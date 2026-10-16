package com.trinhcong1120.notification_service.service;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramService implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final String token;
    private final RestClient client;
    private long nextUpdateId;

    public TelegramService(@Value("${spring.telegram.bot.token:}") String token) {
        this.token = token == null ? "" : token.trim();
        this.client = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + this.token)
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        if (!isEnabled()) {
            log.warn("Telegram notifications are disabled because TELEGRAM_BOT_TOKEN is empty");
            return;
        }
        try {
            JsonNode response = client.post()
                    .uri("/deleteWebhook")
                    .body(Map.of("drop_pending_updates", false))
                    .retrieve()
                    .body(JsonNode.class);
            ensureOk(response, "deleteWebhook");
        } catch (RuntimeException ex) {
            log.warn("Could not remove Telegram webhook; long polling may not work", ex);
        }
    }

    @Scheduled(fixedDelayString = "${spring.telegram.bot.poll-delay-ms:1000}")
    public void poll() {
        if (!isEnabled()) {
            return;
        }
        try {
            JsonNode response = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/getUpdates")
                            .queryParam("offset", nextUpdateId)
                            .queryParam("timeout", 25)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            ensureOk(response, "getUpdates");
            for (JsonNode update : response.path("result")) {
                handleUpdate(update);
                nextUpdateId = update.path("update_id").asLong() + 1;
            }
        } catch (RuntimeException ex) {
            log.warn("Telegram long polling failed", ex);
        }
    }

    void handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        JsonNode chat = message.path("chat");
        if (!"private".equals(chat.path("type").asText())) {
            return;
        }
        String text = message.path("text").asText("").trim();
        String command = text.isEmpty() ? "" : text.split("\\s+", 2)[0];
        if (!command.equals("/start") && !command.startsWith("/start@")) {
            return;
        }
        long chatId = chat.path("id").asLong();
        sendMessage(chatId, "Telegram Chat ID của bạn là: " + chatId
                + "\n\nHãy gửi ID này cho quản trị viên để liên kết với tài khoản hệ thống.");
    }

    public void sendNotification(Long chatId, String subject, String message) {
        if (!isEnabled()) {
            throw new IllegalStateException("Telegram bot token is not configured");
        }
        sendMessage(chatId, subject + "\n\n" + message);
    }

    private void sendMessage(long chatId, String text) {
        JsonNode response = client.post()
                .uri("/sendMessage")
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .body(JsonNode.class);
        ensureOk(response, "sendMessage");
    }

    private boolean isEnabled() {
        return !token.isBlank();
    }

    private void ensureOk(JsonNode response, String operation) {
        if (response == null || !response.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram " + operation + " failed: " + response);
        }
    }
}
