package com.trinhcong1120.notification_service.consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.notification_service.dto.NotificationEvent;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificationConsumerTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final NotificationConsumer consumer = new NotificationConsumer(jdbc);

    @Test
    void storesOneDeliveryPerKafkaRecordWithStableOriginKey() {
        NotificationEvent event = new NotificationEvent("USER_CREATED", "user@example.com", "user",
                "Subject", "Message", UUID.randomUUID());
        event.setRecipientId(UUID.randomUUID());
        ConsumerRecord<String, NotificationEvent> record =
                new ConsumerRecord<>("notification.events", 0, 42, null, event);

        consumer.consume(record);
        consumer.consume(record);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).update(sql.capture(), eq("notification.events:0:42"),
                any(), any(), any(), any(), eq("user@example.com"), eq("EMAIL"),
                eq("Subject"), eq("Message"), eq("USER_CREATED"), any());
        assertTrue(sql.getValue().contains("ON CONFLICT (origin_key) DO NOTHING"));
    }

    @Test
    void ignoresEventWithoutDestination() {
        consumer.consume(new ConsumerRecord<>("notification.events", 0, 1, null,
                new NotificationEvent("INVALID", null, null, "Subject", "Message", UUID.randomUUID())));
        verifyNoInteractions(jdbc);
    }

    @Test
    void producerEventIdRemainsStableAcrossDifferentKafkaOffsets() {
        NotificationEvent event = new NotificationEvent("ALERT", null, "user",
                "Subject", "Message", UUID.randomUUID(), 123L);
        event.setRecipientId(UUID.randomUUID());
        UUID eventId = UUID.randomUUID();
        event.setEventId(eventId);

        consumer.consume(new ConsumerRecord<>("notification.events", 0, 4, null, event));
        consumer.consume(new ConsumerRecord<>("notification.events", 0, 5, null, event));

        verify(jdbc, times(2)).update(anyString(), eq("event:" + eventId),
                any(), any(), any(), any(), eq("123"), eq("TELEGRAM"),
                eq("Subject"), eq("Message"), eq("ALERT"), any());
    }
}
