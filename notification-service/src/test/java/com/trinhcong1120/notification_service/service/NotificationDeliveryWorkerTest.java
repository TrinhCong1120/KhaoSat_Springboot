package com.trinhcong1120.notification_service.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

class NotificationDeliveryWorkerTest {
    @Test
    void retriesTemporaryFailuresOnly() {
        assertTrue(NotificationDeliveryWorker.isTemporary(new RuntimeException("network")));
        assertTrue(NotificationDeliveryWorker.isTemporary(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS)));
        assertFalse(NotificationDeliveryWorker.isTemporary(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));
        assertFalse(NotificationDeliveryWorker.isTemporary(new MailAuthenticationException("bad credentials")));
        RuntimeException timeout = new RuntimeException("timeout");
        assertTrue(NotificationDeliveryWorker.shouldRetry(timeout, 1, 3));
        assertTrue(NotificationDeliveryWorker.shouldRetry(timeout, 2, 3));
        assertFalse(NotificationDeliveryWorker.shouldRetry(timeout, 3, 3));
        assertFalse(NotificationDeliveryWorker.shouldRetry(new MailAuthenticationException("bad credentials"), 1, 3));
    }
}
