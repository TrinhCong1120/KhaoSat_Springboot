package com.trinhcong1120.notification_service.controller;

import com.trinhcong1120.notification_service.service.TelegramService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class TelegramWebhookController {
    private final TelegramService telegram;

    public TelegramWebhookController(TelegramService telegram) {
        this.telegram = telegram;
    }

    @PostMapping("/api/telegram/webhook")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody JsonNode update) {
        String expected = telegram.webhookSecret();
        if (expected.isBlank() || secret == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8)))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        telegram.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
