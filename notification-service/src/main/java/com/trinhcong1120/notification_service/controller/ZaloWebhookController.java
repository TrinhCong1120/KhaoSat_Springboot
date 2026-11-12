package com.trinhcong1120.notification_service.controller;

import com.trinhcong1120.notification_service.service.ZaloService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class ZaloWebhookController {
    private static final Logger log = LoggerFactory.getLogger(ZaloWebhookController.class);
    private final ZaloService zalo;

    public ZaloWebhookController(ZaloService zalo) {
        this.zalo = zalo;
    }

    @PostMapping("/api/zalo/webhook")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody JsonNode update) {
        String expected = zalo.webhookSecret();
        if (expected.isBlank() || secret == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8)))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            zalo.handleUpdate(update);
        } catch (RuntimeException ex) {
            log.warn("Zalo webhook processing failed", ex);
        }
        return ResponseEntity.ok().build();
    }
}
