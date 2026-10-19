package com.trinhcong1120.core_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.trinhcong1120.core_service.exception.BadRequestException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class NotificationInboxManagementServiceTest {
    @Test
    void replyRequiresTheExactInboundDestination() {
        UUID inboundId = UUID.randomUUID();
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var message = new NotificationInboxManagementService.InboxMessage(inboundId, "EMAIL",
                "<mail@example.com>", "sender@example.com", "reply@example.com",
                "system@example.com", null, null, "Question", "Hello", "MESSAGE", "NEW", Instant.now());
        when(jdbc.query(anyString(), any(RowMapper.class), eq(inboundId))).thenReturn(List.of(message));
        var service = new NotificationInboxManagementService(jdbc,
                "http://localhost:9000", "test", "test", "notification-attachments");
        assertThrows(BadRequestException.class, () -> service.reply(inboundId,
                new NotificationInboxManagementService.ReplyRequest("Response", "sender@example.com", null), UUID.randomUUID()));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void regularMessageIsQueuedWithoutReplyLink() {
        UUID inboundId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var message = new NotificationInboxManagementService.InboxMessage(inboundId, "EMAIL",
                "<mail@example.com>", "sender@example.com", "reply@example.com",
                "system@example.com", null, null, "Question", "Hello", "MESSAGE", "NEW", Instant.now());
        when(jdbc.query(anyString(), any(RowMapper.class), eq(inboundId))).thenReturn(List.of(message));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(actorId))).thenReturn(List.of("admin"));
        var service = new NotificationInboxManagementService(jdbc,
                "http://localhost:9000", "test", "test", "notification-attachments");

        service.send(inboundId, new NotificationInboxManagementService.ComposeRequest(
                "New topic", "Hello again", "reply@example.com"), actorId);

        ArgumentCaptor<Object[]> values = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), values.capture());
        Object[] inserted = values.getValue();
        assertEquals("New topic", inserted[7]);
        assertEquals("Hello again", inserted[8]);
        assertNull(inserted[9]);
    }
}
