package com.trinhcong1120.notification_service.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class NotificationDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryWorker.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final EmailService email;
    private final TelegramService telegram;
    private final int maxAttempts;
    private final Duration delay1;
    private final Duration delay2;

    public NotificationDeliveryWorker(JdbcTemplate jdbc, TransactionTemplate transactions,
            EmailService email, TelegramService telegram,
            @Value("${notification.retry.max-attempts:3}") int maxAttempts,
            @Value("${notification.retry.delay-1:PT1M}") Duration delay1,
            @Value("${notification.retry.delay-2:PT5M}") Duration delay2) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.email = email;
        this.telegram = telegram;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.delay1 = delay1;
        this.delay2 = delay2;
    }

    @Scheduled(fixedDelayString = "${notification.worker.poll-delay-ms:1000}")
    public void deliverDue() {
        recoverExpired();
        for (int i = 0; i < 20; i++) {
            Job job = claim();
            if (job == null) return;
            try {
                String providerMessageId;
                if (job.channel().equals("EMAIL")) {
                    providerMessageId = email.sendEmail(job.id(), job.destination(), job.subject(), job.content(), job.replyToProviderId());
                } else {
                    providerMessageId = telegram.sendNotification(Long.parseLong(job.destination()), job.subject(), job.content(), job.replyToProviderId());
                }
                complete(job, null, providerMessageId);
            } catch (RuntimeException error) {
                log.warn("Notification delivery {} attempt {} failed", job.id(), job.attemptNo(), error);
                complete(job, error, null);
            }
        }
    }

    private void recoverExpired() {
        transactions.executeWithoutResult(tx -> {
            jdbc.update("""
                UPDATE notification_attempts SET status = 'FAILED', finished_at = now(),
                    error_note = 'Worker stopped or delivery timed out'
                WHERE status = 'STARTED' AND delivery_id IN
                    (SELECT id FROM notification_deliveries
                     WHERE status = 'PROCESSING' AND lease_until < now())
                """);
            jdbc.update("""
                UPDATE notification_deliveries
                SET status = CASE WHEN attempts >= ? THEN 'FAILED' ELSE 'PENDING' END,
                    next_attempt_at = now(), lease_until = NULL,
                    error_note = 'Worker stopped or delivery timed out'
                WHERE status = 'PROCESSING' AND lease_until < now()
                """, maxAttempts);
        });
    }

    private Job claim() {
        return transactions.execute(tx -> {
            List<Job> jobs = jdbc.query("""
                UPDATE notification_deliveries d
                SET status = 'PROCESSING', attempts = d.attempts + 1,
                    lease_until = now() + interval '2 minutes'
                FROM (SELECT id FROM notification_deliveries
                      WHERE status = 'PENDING' AND next_attempt_at <= now()
                      ORDER BY next_attempt_at, created_at
                      FOR UPDATE SKIP LOCKED LIMIT 1) picked
                WHERE d.id = picked.id
                RETURNING d.id, d.channel, d.destination, d.subject, d.content, d.attempts,
                    (SELECT provider_message_id FROM notification_inbox_messages
                     WHERE id = d.in_reply_to_inbound_id) AS reply_to_provider_id
                """, (rs, n) -> new Job(rs.getObject("id", UUID.class), rs.getString("channel"),
                    rs.getString("destination"), rs.getString("subject"),
                    rs.getString("content"), rs.getInt("attempts"), rs.getString("reply_to_provider_id")));
            if (jobs.isEmpty()) return null;
            Job job = jobs.get(0);
            jdbc.update("""
                INSERT INTO notification_attempts(delivery_id, attempt_no, status)
                VALUES (?, ?, 'STARTED')
                """, job.id(), job.attemptNo());
            return job;
        });
    }

    private void complete(Job job, RuntimeException error, String providerMessageId) {
        transactions.executeWithoutResult(tx -> {
            String note = error == null ? null : errorNote(error);
            boolean retry = shouldRetry(error, job.attemptNo(), maxAttempts);
            String status = error == null ? "SENT" : retry ? "PENDING" : "FAILED";
            Duration wait = job.attemptNo() == 1 ? delay1 : delay2;
            int changed = jdbc.update("""
                UPDATE notification_deliveries
                SET status = ?, error_note = ?, lease_until = NULL,
                    next_attempt_at = ?, sent_at = CASE WHEN ? = 'SENT' THEN now() ELSE sent_at END,
                    provider_message_id = CASE WHEN ? = 'SENT' THEN ? ELSE provider_message_id END
                WHERE id = ? AND status = 'PROCESSING' AND attempts = ?
                """, status, note, Timestamp.from(Instant.now().plus(retry ? wait : Duration.ZERO)),
                    status, status, providerMessageId, job.id(), job.attemptNo());
            if (changed == 0) {
                log.warn("Delivery {} completed after its lease expired", job.id());
                return;
            }
            jdbc.update("""
                UPDATE notification_attempts SET status = ?, finished_at = now(), error_note = ?
                WHERE delivery_id = ? AND attempt_no = ?
                """, error == null ? "SENT" : "FAILED", note, job.id(), job.attemptNo());
        });
    }

    static boolean isTemporary(RuntimeException error) {
        if (error instanceof MailAuthenticationException || error instanceof MailParseException
                || error instanceof IllegalArgumentException || error instanceof IllegalStateException) return false;
        if (error instanceof HttpClientErrorException clientError)
            return clientError.getStatusCode().value() == 429;
        return true;
    }

    static boolean shouldRetry(RuntimeException error, int attemptNo, int maxAttempts) {
        return error != null && attemptNo < maxAttempts && isTemporary(error);
    }

    private static String errorNote(RuntimeException error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        message = message.replace('\n', ' ').replace('\r', ' ');
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private record Job(UUID id, String channel, String destination,
                       String subject, String content, int attemptNo, String replyToProviderId) {}
}
