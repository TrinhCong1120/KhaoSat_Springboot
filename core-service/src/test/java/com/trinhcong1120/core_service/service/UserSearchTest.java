package com.trinhcong1120.core_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.producer.NotificationProducer;
import com.trinhcong1120.core_service.repository.RoleRepository;
import com.trinhcong1120.core_service.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserSearchTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder passwords;
    @Mock NotificationProducer notifications;
    @InjectMocks UserService service;

    @Test
    void exactSearchNormalizesInputAndReturnsMaskedEmail() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("Editor");
        user.setEmail("testuser@gmail.com");
        when(users.searchActiveByExactUsernameOrEmail(
                eq("editor@example.com"), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of(user));

        var results = service.searchActiveUsers("  EDITOR@example.com  ");

        assertEquals(1, results.size());
        assertEquals(user.getId(), results.get(0).id());
        assertEquals("Editor", results.get(0).username());
        assertEquals("test****@gmail.com", results.get(0).email());
        verify(users).searchActiveByExactUsernameOrEmail(
                "editor@example.com", PageRequest.of(0, 20));
    }

    @Test
    void shortOrMissingEmailNeverExposesFullAddress() {
        User shortEmail = new User();
        shortEmail.setEmail("a@gmail.com");
        User missingEmail = new User();
        when(users.searchActiveByExactUsernameOrEmail(
                eq("editor"), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of(shortEmail, missingEmail));

        var results = service.searchActiveUsers("editor");

        assertEquals("****@gmail.com", results.get(0).email());
        assertNull(results.get(1).email());
    }

    @Test
    void rejectsBlankAndOversizedQueriesBeforeAccessingDatabase() {
        assertThrows(BadRequestException.class, () -> service.searchActiveUsers("  "));
        assertThrows(BadRequestException.class, () -> service.searchActiveUsers("x".repeat(256)));
        verifyNoInteractions(users);
    }
}
