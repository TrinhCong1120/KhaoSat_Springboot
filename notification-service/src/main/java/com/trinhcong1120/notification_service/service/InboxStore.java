package com.trinhcong1120.notification_service.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InboxStore {
    private final JdbcTemplate jdbc;
    private final MinioClient minio;
    private final String bucket;
    private final long maxBytes;

    public InboxStore(JdbcTemplate jdbc,
            @Value("${MINIO_ENDPOINT:http://localhost:9000}") String endpoint,
            @Value("${MINIO_ACCESS_KEY:}") String accessKey,
            @Value("${MINIO_SECRET_KEY:}") String secretKey,
            @Value("${notification.inbox.bucket:notification-attachments}") String bucket,
            @Value("${notification.inbox.max-attachment-bytes:20971520}") long maxBytes) {
        this.jdbc = jdbc;
        this.minio = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
        this.maxBytes = maxBytes;
    }

    public UUID save(String channel, String providerKey, String providerMessageId,
            String sender, String recipient, String subject, String content, String kind,
            Instant receivedAt, String replyToMessageId) {
        return save(channel, providerKey, providerMessageId, sender, recipient, subject, content,
                kind, receivedAt, replyToMessageId, sender);
    }

    public UUID save(String channel, String providerKey, String providerMessageId,
            String sender, String recipient, String subject, String content, String kind,
            Instant receivedAt, String replyToMessageId, String replyDestination) {
        UUID id = UUID.randomUUID();
        if (providerKey.length() > 400) throw new IllegalArgumentException("Provider key too long");
        UUID match = null;
        if (replyToMessageId != null && !replyToMessageId.isBlank()) {
            List<UUID> matches = jdbc.query("""
                SELECT id FROM notification_deliveries WHERE channel = ?
                AND CASE WHEN ? = 'EMAIL' THEN lower(destination) = lower(?) ELSE destination = ? END
                AND provider_message_id = ? LIMIT 2
                """, (rs, n) -> rs.getObject(1, UUID.class), channel, channel, sender, sender, replyToMessageId);
            if (matches.size() == 1) match = matches.get(0);
        }
        jdbc.update("""
            INSERT INTO notification_inbox_messages
            (id,channel,provider_key,provider_message_id,sender_address,reply_destination,recipient_address,
             sender_user_id,matched_delivery_id,subject,content,kind,received_at)
            VALUES (?,?,?,?,?,?,?,(SELECT id FROM users WHERE
                CASE WHEN ? = 'EMAIL' THEN lower(email) = lower(?) ELSE telegram_chat_id::text = ? END
                LIMIT 1),?,?,?,?,?)
            ON CONFLICT (provider_key) DO NOTHING
            """, id, channel, providerKey, limit(providerMessageId, 500), limit(sender, 500),
                limit(replyDestination, 500), limit(recipient, 500),
                channel, sender, sender, match, limit(subject, 500), content, kind, Timestamp.from(receivedAt));
        return jdbc.queryForObject("SELECT id FROM notification_inbox_messages WHERE provider_key = ?", UUID.class, providerKey);
    }

    public void attachment(UUID inboxId, String name, String type, long size, InputStream content) {
        attachment(inboxId, name + ":" + size, name, type, size, content);
    }

    public void attachment(UUID inboxId, String attachmentKey, String name, String type,
            long size, InputStream content) {
        attachmentKey = limit(attachmentKey, 100);
        name = limit(name, 500);
        type = limit(type, 200);
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM notification_inbox_attachments WHERE inbox_id = ? AND attachment_key = ?", Integer.class, inboxId, attachmentKey);
        if (existing != null && existing > 0) return;
        UUID id = UUID.randomUUID();
        String key = inboxId + "/" + id;
        String status = "STORED";
        String error = null;
        if (size > maxBytes) {
            status = "OVERSIZE";
            error = "Tệp vượt giới hạn " + maxBytes + " byte";
            key = null;
        } else {
            try {
                if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
                    minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                minio.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                        .stream(content, size, -1).contentType(type == null ? "application/octet-stream" : type).build());
            } catch (Exception ex) {
                status = "FAILED";
                error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                if (error.length() > 500) error = error.substring(0, 500);
                key = null;
            }
        }
        jdbc.update("""
            INSERT INTO notification_inbox_attachments
            (id,inbox_id,attachment_key,file_name,content_type,size_bytes,storage_key,status,error_note)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, id, inboxId, attachmentKey, name, type, size, key, status, error);
    }

    public long maxBytes() { return maxBytes; }

    public void failedAttachment(UUID inboxId, String name, String type, long size, String reason) {
        failedAttachment(inboxId, name + ":" + size, name, type, size, reason);
    }

    public void failedAttachment(UUID inboxId, String attachmentKey, String name, String type,
            long size, String reason) {
        jdbc.update("""
            INSERT INTO notification_inbox_attachments
            (id,inbox_id,attachment_key,file_name,content_type,size_bytes,status,error_note)
            VALUES (?,?,?,?,?,?,'FAILED',?) ON CONFLICT DO NOTHING
            """, UUID.randomUUID(), inboxId, limit(attachmentKey, 100), limit(name, 500), limit(type, 200), size,
                reason.substring(0, Math.min(500, reason.length())));
    }

    private static String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
