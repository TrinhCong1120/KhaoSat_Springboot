package com.trinhcong1120.notification_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trinhcong1120.notification_service.service.TelegramService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

class TelegramWebhookControllerTest {
    @Test
    void rejectsMissingOrWrongSecret() {
        TelegramService telegram = mock(TelegramService.class);
        when(telegram.webhookSecret()).thenReturn("secret_123");
        TelegramWebhookController controller = new TelegramWebhookController(telegram);
        JsonNode update = mock(JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, controller.receive(null, update).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.receive("wrong", update).getStatusCode());
        verifyNoInteractions(update);
    }

    @Test
    void acceptsMatchingSecretAndPassesUpdate() {
        TelegramService telegram = mock(TelegramService.class);
        when(telegram.webhookSecret()).thenReturn("secret_123");
        JsonNode update = mock(JsonNode.class);

        assertEquals(HttpStatus.OK, new TelegramWebhookController(telegram)
                .receive("secret_123", update).getStatusCode());
        verify(telegram).handleUpdate(update);
    }
}
