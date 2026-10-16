package com.trinhcong1120.core_service.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
import org.junit.jupiter.api.Test;

class UserTelegramChatIdTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void updateDistinguishesOmittedValueFromExplicitNull() throws Exception {
        UpdateUserRequest omitted = mapper.readValue("{}", UpdateUserRequest.class);
        UpdateUserRequest cleared = mapper.readValue("{\"telegramChatId\":null}", UpdateUserRequest.class);
        UpdateUserRequest linked = mapper.readValue("{\"telegramChatId\":123456789}", UpdateUserRequest.class);

        assertFalse(omitted.isTelegramChatIdPresent());
        assertTrue(cleared.isTelegramChatIdPresent());
        assertNull(cleared.getTelegramChatId());
        assertTrue(linked.isTelegramChatIdPresent());
        assertEquals(123456789L, linked.getTelegramChatId());
    }
}
