package com.trinhcong1120.core_service.producer;

import com.trinhcong1120.core_service.dto.event.NotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private static final String TOPIC =
            "notification.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationProducer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(NotificationEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event
        );

        System.out.println(
                "Đã gửi Kafka event: "
                        + event.getEventType()
        );
    }
}