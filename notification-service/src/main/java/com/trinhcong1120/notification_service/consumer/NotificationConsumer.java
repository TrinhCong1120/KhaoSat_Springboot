package com.trinhcong1120.notification_service.consumer;

import com.trinhcong1120.notification_service.dto.NotificationEvent;
import com.trinhcong1120.notification_service.service.EmailService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private final EmailService emailService;

    public NotificationConsumer(
            EmailService emailService
    ) {
        this.emailService = emailService;
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

        emailService.sendEmail(
                event.getEmail(),
                event.getSubject(),
                event.getMessage()
        );

        System.out.println(
                "================================"
        );
    }
}