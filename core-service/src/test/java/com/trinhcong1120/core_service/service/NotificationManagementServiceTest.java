package com.trinhcong1120.core_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificationManagementServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationManagementService service = new NotificationManagementService(jdbc, users);

    @Test
    void queuesOneDeliveryPerRecipientAndChannel() {
        UUID actorId = UUID.randomUUID();
        User actor = user(actorId, "admin", "admin@example.com", 99L);
        User first = user(UUID.randomUUID(), "first", "first@example.com", 11L);
        User second = user(UUID.randomUUID(), "second", "second@example.com", 22L);
        when(users.findByIdAndIsActiveTrue(actorId)).thenReturn(Optional.of(actor));
        when(users.findAllById(any())).thenReturn(List.of(first, second));

        var created = service.create(new NotificationManagementService.CreateRequest(
                List.of(first.getId(), second.getId()), List.of("EMAIL", "TELEGRAM"),
                "Subject", "Body"), actorId);

        assertEquals(4, created.ids().size());
        verify(jdbc, times(4)).update(contains("INSERT INTO notification_deliveries"),
                any(), eq(actorId), eq("admin"), any(), any(), any(), any(),
                eq("Subject"), eq("Body"));
    }

    @Test
    void rejectsRecipientWithoutSelectedChannel() {
        UUID actorId = UUID.randomUUID();
        User actor = user(actorId, "admin", "admin@example.com", 99L);
        User recipient = user(UUID.randomUUID(), "recipient", null, 11L);
        when(users.findByIdAndIsActiveTrue(actorId)).thenReturn(Optional.of(actor));
        when(users.findAllById(any())).thenReturn(List.of(recipient));

        assertThrows(BadRequestException.class, () -> service.create(
                new NotificationManagementService.CreateRequest(List.of(recipient.getId()),
                        List.of("EMAIL"), "Subject", "Body"), actorId));
        verifyNoInteractions(jdbc);
    }

    private static User user(UUID id, String name, String email, Long chatId) {
        User user = new User();
        user.setId(id);
        user.setUsername(name);
        user.setEmail(email);
        user.setTelegramChatId(chatId);
        user.setIsActive(true);
        return user;
    }
}
