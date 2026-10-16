package com.trinhcong1120.notification_service.consumer;

import static org.mockito.Mockito.*;

import com.trinhcong1120.notification_service.dto.NotificationEvent;
import com.trinhcong1120.notification_service.service.EmailService;
import com.trinhcong1120.notification_service.service.TelegramService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationConsumerTest {
    private final EmailService email = mock(EmailService.class);
    private final TelegramService telegram = mock(TelegramService.class);
    private final NotificationConsumer consumer = new NotificationConsumer(email, telegram);

    @Test
    void routesEachKafkaRecordToOnlyOneChannel() {
        consumer.consume(new NotificationEvent("USER_CREATED", "user@example.com", "user",
                "Subject", "Message", UUID.randomUUID()));
        consumer.consume(new NotificationEvent("USER_CREATED", null, "user",
                "Subject", "Message", UUID.randomUUID(), 123L));

        verify(email).sendEmail("user@example.com", "Subject", "Message");
        verify(telegram).sendNotification(123L, "Subject", "Message");
        verifyNoMoreInteractions(email, telegram);
    }

    @Test
    void ignoresEventWithoutDestination() {
        consumer.consume(new NotificationEvent("INVALID", null, null,
                "Subject", "Message", UUID.randomUUID()));

        verifyNoInteractions(email, telegram);
    }
}
