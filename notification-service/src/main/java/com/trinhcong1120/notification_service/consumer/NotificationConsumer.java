package com.trinhcong1120.notification_service.consumer;

import com.trinhcong1120.notification_service.dto.NotificationEvent;
import com.trinhcong1120.notification_service.service.EmailService;
import com.trinhcong1120.notification_service.service.TelegramService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final EmailService emailService;
    private final TelegramService telegramService;

    public NotificationConsumer(
            EmailService emailService,
            TelegramService telegramService
    ) {
        this.emailService = emailService;
        this.telegramService = telegramService;
    }

    @KafkaListener(
            topics = "notification.events",
            groupId = "notification-service"
    )
    public void consume(
            NotificationEvent event
    ) {

        System.out.println(
                "================================"
        );

        System.out.println(
                "Nhận Kafka event: "
                        + event.getEventType()
        );

        System.out.println(
                "Email: "
                        + event.getEmail()
        );

        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            emailService.sendEmail(event.getEmail(), event.getSubject(), event.getMessage());
        } else if (event.getTelegramChatId() != null) {
            telegramService.sendNotification(event.getTelegramChatId(),
                    event.getSubject(), event.getMessage());
        } else {
            log.warn("Ignoring notification event {} without a destination", event.getEventType());
        }

        System.out.println(
                "================================"
        );
    }
}
