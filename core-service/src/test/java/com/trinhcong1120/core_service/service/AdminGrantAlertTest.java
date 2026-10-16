package com.trinhcong1120.core_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.trinhcong1120.core_service.dto.event.NotificationEvent;
import com.trinhcong1120.core_service.dto.user.CreateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.producer.NotificationProducer;
import com.trinhcong1120.core_service.repository.RoleRepository;
import com.trinhcong1120.core_service.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AdminGrantAlertTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final NotificationProducer producer = mock(NotificationProducer.class);
    private final AdminGrantAlertService alerts = new AdminGrantAlertService(users, producer);
    private final UserService service = new UserService(users, roles, mock(PasswordEncoder.class), producer, alerts);

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void newAdminGrantSendsOneEmailPerAddressOnlyAfterCommit() {
        UUID actorId = UUID.randomUUID(), targetId = UUID.randomUUID(), roleId = UUID.randomUUID();
        User actor = user(actorId, "operator");
        User target = user(targetId, "new-admin");
        Role admin = new Role(); admin.setName("aDmIn"); admin.setId(roleId);
        when(users.findById(targetId)).thenReturn(java.util.Optional.of(target));
        when(users.findById(actorId)).thenReturn(java.util.Optional.of(actor));
        when(roles.findAllById(List.of(roleId))).thenReturn(List.of(admin));
        when(users.save(target)).thenReturn(target);
        User first = user(UUID.randomUUID(), "first"); first.setEmail("admin@example.com"); first.setTelegramChatId(101L);
        User duplicate = user(UUID.randomUUID(), "duplicate"); duplicate.setEmail(" ADMIN@example.com "); duplicate.setTelegramChatId(101L);
        User second = user(UUID.randomUUID(), "second"); second.setEmail("second@example.com"); second.setTelegramChatId(202L);
        when(users.findActiveAdmins()).thenReturn(List.of(first, duplicate, second));
        UpdateUserRequest request = new UpdateUserRequest(); request.setRoleIds(List.of(roleId));

        TransactionSynchronizationManager.initSynchronization();
        service.updateUser(targetId, request, actorId);
        verifyNoInteractions(producer);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

        var events = org.mockito.ArgumentCaptor.forClass(NotificationEvent.class);
        verify(producer, times(4)).send(events.capture());
        assertEquals(List.of("admin@example.com", "second@example.com"),
                events.getAllValues().stream().map(NotificationEvent::getEmail)
                        .filter(java.util.Objects::nonNull).toList());
        assertEquals(List.of(101L, 202L), events.getAllValues().stream()
                .map(NotificationEvent::getTelegramChatId).filter(java.util.Objects::nonNull).toList());
        assertTrue(events.getValue().getMessage().contains("operator"));
        assertTrue(events.getValue().getMessage().contains("new-admin"));
        assertTrue(events.getValue().getMessage().contains("GMT+7"));
        assertEquals("ADMIN_ROLE_GRANTED", events.getValue().getEventType());
    }

    @Test
    void existingAdminDoesNotSendAnotherGrantAlert() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, "existing-admin");
        Role admin = new Role(); admin.setName("Admin");
        target.setRoles(Set.of(admin));
        when(users.findById(targetId)).thenReturn(java.util.Optional.of(target));
        when(users.save(target)).thenReturn(target);

        service.updateUser(targetId, new UpdateUserRequest(), UUID.randomUUID());

        verify(users, never()).findActiveAdmins();
        verifyNoInteractions(producer);
    }

    @Test
    void creatingAdminIncludesNewAccountInRecipients() {
        UUID actorId = UUID.randomUUID(), roleId = UUID.randomUUID(), newUserId = UUID.randomUUID();
        Role admin = new Role(); admin.setId(roleId); admin.setName("Admin");
        when(roles.findAllById(List.of(roleId))).thenReturn(List.of(admin));
        when(users.save(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            saved.setId(newUserId);
            return saved;
        });
        when(users.findById(actorId)).thenReturn(java.util.Optional.of(user(actorId, "operator")));
        User recipient = user(newUserId, "new-admin"); recipient.setEmail("new-admin@example.com");
        when(users.findActiveAdmins()).thenReturn(List.of(recipient));
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("new-admin"); request.setEmail("new-admin@example.com");
        request.setPassword("password123"); request.setRoleIds(List.of(roleId));

        TransactionSynchronizationManager.initSynchronization();
        assertEquals(newUserId, service.createUser(request, actorId));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

        var event = org.mockito.ArgumentCaptor.forClass(NotificationEvent.class);
        verify(producer, atLeastOnce()).send(event.capture());
        assertTrue(event.getAllValues().stream().anyMatch(e ->
                "ADMIN_ROLE_GRANTED".equals(e.getEventType())
                        && "new-admin@example.com".equals(e.getEmail())));
    }

    @Test
    void updateRejectsInvalidOrAlreadyLinkedTelegramChatId() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, "target");
        when(users.findById(targetId)).thenReturn(java.util.Optional.of(target));

        UpdateUserRequest invalid = new UpdateUserRequest();
        invalid.setTelegramChatId(-1L);
        assertThrows(BadRequestException.class,
                () -> service.updateUser(targetId, invalid, UUID.randomUUID()));

        UpdateUserRequest duplicate = new UpdateUserRequest();
        duplicate.setTelegramChatId(123L);
        when(users.existsByTelegramChatIdAndIdNot(123L, targetId)).thenReturn(true);
        assertThrows(BadRequestException.class,
                () -> service.updateUser(targetId, duplicate, UUID.randomUUID()));

        verify(users, never()).save(any());
    }

    @Test
    void linkingTelegramSendsMaskedAccountConfirmationAfterCommit() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, "alice");
        target.setEmail("alice.long@example.com");
        when(users.findById(targetId)).thenReturn(java.util.Optional.of(target));
        when(users.save(target)).thenReturn(target);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setTelegramChatId(123456L);

        TransactionSynchronizationManager.initSynchronization();
        service.updateUser(targetId, request, UUID.randomUUID());
        verifyNoInteractions(producer);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

        var event = org.mockito.ArgumentCaptor.forClass(NotificationEvent.class);
        verify(producer).send(event.capture());
        assertEquals("TELEGRAM_ACCOUNT_LINKED", event.getValue().getEventType());
        assertEquals(123456L, event.getValue().getTelegramChatId());
        assertTrue(event.getValue().getMessage().contains("alice"));
        assertTrue(event.getValue().getMessage().contains("alic****@example.com"));
    }

    @Test
    void roleChangesTellLinkedTelegramWhichRolesWereAddedAndRemoved() {
        UUID targetId = UUID.randomUUID(), editorId = UUID.randomUUID();
        User target = user(targetId, "alice");
        target.setTelegramChatId(456789L);
        Role viewer = new Role(); viewer.setName("Viewer");
        Role editor = new Role(); editor.setId(editorId); editor.setName("Editor");
        target.setRoles(Set.of(viewer));
        when(users.findById(targetId)).thenReturn(java.util.Optional.of(target));
        when(roles.findAllById(List.of(editorId))).thenReturn(List.of(editor));
        when(users.save(target)).thenReturn(target);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRoleIds(List.of(editorId));

        TransactionSynchronizationManager.initSynchronization();
        service.updateUser(targetId, request, UUID.randomUUID());
        verifyNoInteractions(producer);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());

        var event = org.mockito.ArgumentCaptor.forClass(NotificationEvent.class);
        verify(producer).send(event.capture());
        assertEquals("USER_ROLES_CHANGED", event.getValue().getEventType());
        assertEquals(456789L, event.getValue().getTelegramChatId());
        assertTrue(event.getValue().getMessage().contains("Quyền được thêm: Editor"));
        assertTrue(event.getValue().getMessage().contains("Quyền bị gỡ: Viewer"));
    }

    private User user(UUID id, String name) {
        User user = new User(); user.setId(id); user.setUsername(name); user.setIsActive(true);
        return user;
    }
}
