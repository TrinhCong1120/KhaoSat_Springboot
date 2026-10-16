package com.trinhcong1120.core_service.service;

import com.trinhcong1120.core_service.dto.event.NotificationEvent;
import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.producer.NotificationProducer;
import com.trinhcong1120.core_service.repository.UserRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AdminGrantAlertService {
    private static final DateTimeFormatter VIETNAM_TIME = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss 'GMT+7'")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final UserRepository users;
    private final NotificationProducer producer;

    public AdminGrantAlertService(UserRepository users, NotificationProducer producer) {
        this.users = users;
        this.producer = producer;
    }

    public void notifyGrant(User target, UUID actorId) {
        User actor = users.findById(actorId).orElseThrow();
        LinkedHashMap<String, String> uniqueEmails = new LinkedHashMap<>();
        LinkedHashSet<Long> uniqueChatIds = new LinkedHashSet<>();
        for (User admin : users.findActiveAdmins()) {
            String email = admin.getEmail();
            if (email != null && !email.isBlank()) {
                String trimmed = email.trim();
                uniqueEmails.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
            if (admin.getTelegramChatId() != null) {
                uniqueChatIds.add(admin.getTelegramChatId());
            }
        }
        String message = "Tài khoản " + target.getUsername() + " (" + target.getId()
                + ") đã được cấp role Admin.\nNgười thực hiện: " + actor.getUsername()
                + " (" + actor.getId() + ").\nThời điểm: " + VIETNAM_TIME.format(Instant.now()) + ".";
        List<NotificationEvent> events = new ArrayList<>();
        uniqueEmails.values().forEach(email -> events.add(new NotificationEvent(
                "ADMIN_ROLE_GRANTED", email, target.getUsername(),
                "Cảnh báo: cấp quyền Admin", message, target.getId())));
        uniqueChatIds.forEach(chatId -> events.add(new NotificationEvent(
                "ADMIN_ROLE_GRANTED", null, target.getUsername(),
                "Cảnh báo: cấp quyền Admin", message, target.getId(), chatId)));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(producer::send);
            }
        });
    }
}
