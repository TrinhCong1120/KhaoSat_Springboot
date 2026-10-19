package com.trinhcong1120.notification_service.service;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailBounceMonitorTest {
    @Test
    void readsHtmlOnlyEmailAsTextWithoutScriptContent() throws Exception {
        String raw = "MIME-Version: 1.0\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n"
                + "<h1>Xin chào</h1><p>Phản hồi<br>của tôi</p><script>steal()</script>";
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        String body = EmailBounceMonitor.body(message);
        assertTrue(body.contains("Xin chào"));
        assertTrue(body.contains("Phản hồi\ncủa tôi"));
        assertFalse(body.contains("<h1>"));
        assertFalse(body.contains("steal()"));
    }
    @Test
    void matchesFailedDsnToOriginalDeliveryHeader() throws Exception {
        UUID id = UUID.randomUUID();
        String raw = """
            From: Mail Delivery Subsystem <mailer-daemon@googlemail.com>
            MIME-Version: 1.0
            Content-Type: multipart/report; report-type=delivery-status; boundary="bounce"

            --bounce
            Content-Type: text/plain; charset=UTF-8

            Delivery failed.
            --bounce
            Content-Type: message/delivery-status

            Reporting-MTA: dns; googlemail.com

            Final-Recipient: rfc822; testuser@example.com
            Action: failed
            Status: 5.1.2
            Diagnostic-Code: smtp; DNS Error: Null MX

            --bounce
            Content-Type: message/rfc822

            From: sender@gmail.com
            To: testuser@example.com
            Subject: Admin alert
            X-Notification-Delivery-Id: %s
            Content-Type: text/plain

            Original message.
            --bounce--
            """.formatted(id).replace("\n", "\r\n");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        EmailBounceMonitor.Bounce bounce = EmailBounceMonitor.parse(message);
        assertNotNull(bounce);
        assertEquals(id, bounce.deliveryId());
        assertEquals("testuser@example.com", bounce.recipient());
        assertTrue(bounce.reason().contains("Null MX"));
    }

    @Test
    void ignoresOrdinaryEmail() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setText("Action: failed\nFinal-Recipient: rfc822; someone@example.com");
        assertNull(EmailBounceMonitor.parse(message));
    }

    @Test
    void extractsOriginalHeadersForOlderMailWithoutDeliveryId() throws Exception {
        String raw = """
            From: mailer-daemon@googlemail.com
            MIME-Version: 1.0
            Content-Type: multipart/report; report-type=delivery-status; boundary="bounce"

            --bounce
            Content-Type: message/delivery-status

            Reporting-MTA: dns; googlemail.com

            Final-Recipient: rfc822; testuser@example.com
            Action: failed
            Status: 5.1.2

            --bounce
            Content-Type: message/rfc822

            From: sender@gmail.com
            To: testuser@example.com
            Subject: Admin alert

            Original message.
            --bounce--
            """.replace("\n", "\r\n");
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        EmailBounceMonitor.Bounce bounce = EmailBounceMonitor.parse(message);
        assertNotNull(bounce);
        assertNull(bounce.deliveryId());
        assertEquals("Admin alert", bounce.originalSubject());
        assertEquals("sender@gmail.com", bounce.originalSender());
    }
}
