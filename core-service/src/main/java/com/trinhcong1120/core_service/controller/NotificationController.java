package com.trinhcong1120.core_service.controller;

import com.trinhcong1120.core_service.service.NotificationManagementService;
import com.trinhcong1120.core_service.service.NotificationManagementService.CreateRequest;
import com.trinhcong1120.core_service.service.NotificationInboxManagementService;
import com.trinhcong1120.core_service.service.NotificationInboxManagementService.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationManagementService notifications;
    private final NotificationInboxManagementService inbox;

    public NotificationController(NotificationManagementService notifications, NotificationInboxManagementService inbox) {
        this.notifications = notifications;
        this.inbox = inbox;
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasAuthority('notification_view')")
    public Object inbox(@RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean unmatched) {
        return inbox.list(page, channel, status, q, unmatched);
    }

    @GetMapping("/inbox/{id}")
    @PreAuthorize("hasAuthority('notification_view')")
    public Object inboxDetail(@PathVariable UUID id) { return inbox.detail(id); }

    @GetMapping("/inbox/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('notification_view')")
    public ResponseEntity<byte[]> attachment(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        Download file = inbox.download(id, attachmentId);
        String safe = file.name().replaceAll("[\\r\\n]", "_");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(safe, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType() == null ? "application/octet-stream" : file.contentType()))
                .body(file.bytes());
    }

    @PostMapping("/inbox/{id}/reply")
    @PreAuthorize("hasAuthority('notification_send')")
    public ResponseEntity<?> reply(@PathVariable UUID id, @RequestBody ReplyRequest request, Authentication auth) {
        return ResponseEntity.accepted().body(java.util.Map.of("id", inbox.reply(id, request, UUID.fromString(auth.getName()))));
    }

    @PostMapping("/inbox/{id}/send")
    @PreAuthorize("hasAuthority('notification_send')")
    public ResponseEntity<?> sendInConversation(@PathVariable UUID id, @RequestBody ComposeRequest request, Authentication auth) {
        return ResponseEntity.accepted().body(java.util.Map.of("id", inbox.send(id, request, UUID.fromString(auth.getName()))));
    }

    @PostMapping("/inbox/{id}/link")
    @PreAuthorize("hasAuthority('notification_send')")
    public ResponseEntity<Void> link(@PathVariable UUID id, @RequestBody LinkRequest request) {
        inbox.link(id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/inbox/{id}/status")
    @PreAuthorize("hasAuthority('notification_send')")
    public ResponseEntity<Void> inboxStatus(@PathVariable UUID id, @RequestBody StatusRequest request) {
        inbox.status(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('notification_view')")
    public Object list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String channel,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Instant from,
                       @RequestParam(required = false) Instant to) {
        return notifications.list(page, status, channel, q, from, to);
    }

    @GetMapping("/chats")
    @PreAuthorize("hasAuthority('notification_view')")
    public Object chats(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(required = false) Long chatId) {
        return notifications.chats(page, chatId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('notification_view')")
    public Object detail(@PathVariable UUID id) {
        return notifications.detail(id);
    }

    @GetMapping("/recipients")
    @PreAuthorize("hasAuthority('notification_send')")
    public Object recipients(@RequestParam(defaultValue = "") String q) {
        return notifications.recipients(q);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('notification_send')")
    public ResponseEntity<?> create(@RequestBody CreateRequest request, Authentication auth) {
        return ResponseEntity.accepted().body(notifications.create(request, UUID.fromString(auth.getName())));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('notification_send')")
    public ResponseEntity<?> retry(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.accepted().body(notifications.retry(id, UUID.fromString(auth.getName())));
    }
}
