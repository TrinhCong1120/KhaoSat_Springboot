package com.trinhcong1120.core_service.service;

import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.exception.NotFoundException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationInboxManagementService {
    public record InboxMessage(UUID id, String channel, String providerMessageId, String senderAddress,
            String replyDestination, String recipientAddress, UUID senderUserId, UUID matchedDeliveryId, String subject,
            String content, String kind, String handlingStatus, Instant receivedAt) {}
    public record Attachment(UUID id, String fileName, String contentType, Long sizeBytes,
            String status, String errorNote) {}
    public record ConversationItem(String direction, UUID id, String content, String subject,
            String status, Instant at, String errorNote) {}
    public record Detail(InboxMessage message, List<Attachment> attachments,
            List<ConversationItem> conversation) {}
    public record Page(List<InboxMessage> content, int page, long totalElements, int totalPages) {}
    public record ReplyRequest(String content, String confirmedDestination, String subject) {}
    public record ComposeRequest(String subject, String content, String confirmedDestination) {}
    public record LinkRequest(UUID deliveryId) {}
    public record StatusRequest(String status) {}
    public record Download(byte[] bytes, String name, String contentType) {}

    private final JdbcTemplate jdbc;
    private final MinioClient minio;
    private final String bucket;

    public NotificationInboxManagementService(JdbcTemplate jdbc,
            @Value("${MINIO_ENDPOINT:http://localhost:9000}") String endpoint,
            @Value("${MINIO_ACCESS_KEY:}") String access,
            @Value("${MINIO_SECRET_KEY:}") String secret,
            @Value("${NOTIFICATION_ATTACHMENT_BUCKET:notification-attachments}") String bucket) {
        this.jdbc = jdbc;
        this.minio = MinioClient.builder().endpoint(endpoint).credentials(access, secret).build();
        this.bucket = bucket;
    }

    private static final String SELECT = """
        SELECT id,channel,provider_message_id,sender_address,reply_destination,recipient_address,sender_user_id,
               matched_delivery_id,subject,content,kind,handling_status,received_at
        FROM notification_inbox_messages
        """;

    @Transactional(readOnly = true)
    public Page list(int page, String channel, String status, String q, boolean unmatched) {
        if (page < 1 || page > 100000) throw new BadRequestException("Trang không hợp lệ");
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (channel != null && !channel.isBlank()) {
            if (!List.of("EMAIL", "TELEGRAM").contains(channel)) throw new BadRequestException("Kênh không hợp lệ");
            where.append(" AND channel = ?"); args.add(channel);
        }
        if (status != null && !status.isBlank()) {
            if (!List.of("NEW", "RESOLVED").contains(status)) throw new BadRequestException("Trạng thái không hợp lệ");
            where.append(" AND handling_status = ?"); args.add(status);
        }
        if (q != null && !q.isBlank()) {
            if (q.length() > 100) throw new BadRequestException("Từ khóa quá dài");
            where.append(" AND (sender_address ILIKE ? OR subject ILIKE ?)");
            String search = "%" + q.trim().replace("%", "\\%")
                    .replace("_", "\\_") + "%";
            args.add(search); args.add(search);
        }
        if (unmatched) where.append(" AND matched_delivery_id IS NULL");
        Long total = jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT 1 FROM notification_inbox_messages
                """ + where + " GROUP BY channel, lower(sender_address)) chats", Long.class, args.toArray());
        args.add(20); args.add((page - 1) * 20);
        List<InboxMessage> rows = jdbc.query("""
                SELECT * FROM (
                    SELECT DISTINCT ON (channel, lower(sender_address))
                           id,channel,provider_message_id,sender_address,reply_destination,
                           recipient_address,sender_user_id,matched_delivery_id,subject,content,
                           kind,handling_status,received_at
                    FROM notification_inbox_messages
                """ + where + """
                    ORDER BY channel,lower(sender_address),received_at DESC,id DESC
                ) chats ORDER BY received_at DESC,id DESC LIMIT ? OFFSET ?
                """, (rs, n) -> map(rs), args.toArray());
        long count = total == null ? 0 : total;
        return new Page(rows, page, count, (int) ((count + 19) / 20));
    }

    @Transactional(readOnly = true)
    public Detail detail(UUID id) {
        InboxMessage message = find(id);
        List<Attachment> attachments = jdbc.query("""
            SELECT id,file_name,content_type,size_bytes,status,error_note
            FROM notification_inbox_attachments WHERE inbox_id = ? ORDER BY created_at,id
            """, (rs, n) -> new Attachment(rs.getObject("id", UUID.class), rs.getString("file_name"),
                rs.getString("content_type"), rs.getObject("size_bytes", Long.class),
                rs.getString("status"), rs.getString("error_note")), id);
        List<ConversationItem> conversation = jdbc.query("""
            SELECT 'IN' AS direction,id,content,subject,handling_status AS status,
                   received_at AS at,NULL::text AS error_note
            FROM notification_inbox_messages WHERE channel = ? AND lower(sender_address) = lower(?)
            UNION ALL
            SELECT 'OUT',id,content,subject,status,created_at,error_note
            FROM notification_deliveries WHERE channel = ? AND (lower(destination) = lower(?) OR lower(destination) = lower(?))
            ORDER BY at DESC LIMIT 100
            """, (rs, n) -> new ConversationItem(rs.getString("direction"), rs.getObject("id", UUID.class),
                rs.getString("content"), rs.getString("subject"), rs.getString("status"),
                time(rs, "at"), rs.getString("error_note")),
                message.channel(), message.senderAddress(), message.channel(), message.senderAddress(),
                destination(message));
        return new Detail(message, attachments, conversation);
    }

    @Transactional
    public UUID reply(UUID id, ReplyRequest request, UUID actorId) {
        InboxMessage incoming = find(id);
        if (incoming.kind().equals("BOUNCE")) throw new BadRequestException("Không thể trả lời thư hoàn lại");
        String subject = request == null || request.subject() == null || request.subject().isBlank()
                ? incoming.channel().equals("EMAIL")
                    ? "Re: " + (incoming.subject() == null || incoming.subject().isBlank() ? "Thông báo" : incoming.subject().replaceFirst("(?i)^Re: *", ""))
                    : "Phản hồi"
                : request.subject();
        return queue(incoming, subject, request == null ? null : request.content(),
                request == null ? null : request.confirmedDestination(), actorId, id);
    }

    @Transactional
    public UUID send(UUID id, ComposeRequest request, UUID actorId) {
        InboxMessage incoming = find(id);
        if (incoming.kind().equals("BOUNCE")) throw new BadRequestException("Không thể gửi tin tới địa chỉ thư hoàn lại");
        return queue(incoming, request == null ? null : request.subject(),
                request == null ? null : request.content(),
                request == null ? null : request.confirmedDestination(), actorId, null);
    }

    private UUID queue(InboxMessage incoming, String subject, String content, String confirmedDestination,
            UUID actorId, UUID inReplyToInboundId) {
        if (subject == null || subject.isBlank() || subject.trim().length() > 500
                || content == null || content.isBlank() || content.trim().length() > 10000)
            throw new BadRequestException("Tiêu đề hoặc nội dung không hợp lệ");
        if (!destination(incoming).equals(confirmedDestination))
            throw new BadRequestException("Cần xác nhận đúng địa chỉ hoặc Chat ID sẽ nhận tin");
        if (incoming.channel().equals("EMAIL") && !destination(incoming).matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            throw new BadRequestException("Địa chỉ email không hợp lệ");
        String actor = jdbc.query("SELECT username FROM users WHERE id = ? AND is_active IS TRUE",
                (rs, n) -> rs.getString(1), actorId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Người gửi không tồn tại"));
        UUID deliveryId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO notification_deliveries
            (id,source,actor_user_id,actor_name,recipient_user_id,recipient_name,destination,
             channel,subject,content,in_reply_to_inbound_id)
            VALUES (?,'MANUAL',?,?,?,?,?,?,?,?,?)
        """, deliveryId, actorId, actor, incoming.senderUserId(), incoming.senderAddress(),
                destination(incoming), incoming.channel(), subject.trim(), content.trim(), inReplyToInboundId);
        return deliveryId;
    }

    @Transactional
    public void link(UUID id, LinkRequest request) {
        InboxMessage message = find(id);
        if (request == null || request.deliveryId() == null) throw new BadRequestException("Thiếu lượt gửi");
        Integer count = jdbc.queryForObject("SELECT count(*) FROM notification_deliveries WHERE id = ? AND channel = ? AND destination IN (?, ?)",
                Integer.class, request.deliveryId(), message.channel(), message.senderAddress(), destination(message));
        if (count == null || count != 1) throw new BadRequestException("Lượt gửi không cùng kênh hoặc người nhận");
        jdbc.update("UPDATE notification_inbox_messages SET matched_delivery_id = ? WHERE id = ?", request.deliveryId(), id);
    }

    @Transactional
    public void status(UUID id, StatusRequest request) {
        find(id);
        if (request == null || !List.of("NEW", "RESOLVED").contains(request.status()))
            throw new BadRequestException("Trạng thái không hợp lệ");
        jdbc.update("UPDATE notification_inbox_messages SET handling_status = ? WHERE id = ?", request.status(), id);
    }

    public Download download(UUID inboxId, UUID attachmentId) {
        find(inboxId);
        var rows = jdbc.query("""
            SELECT file_name,content_type,storage_key FROM notification_inbox_attachments
            WHERE id = ? AND inbox_id = ? AND status = 'STORED'
            """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}, attachmentId, inboxId);
        if (rows.isEmpty()) throw new NotFoundException("Tệp không tồn tại hoặc chưa tải được");
        try (var stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(rows.get(0)[2]).build())) {
            return new Download(stream.readAllBytes(), rows.get(0)[0], rows.get(0)[1]);
        } catch (Exception ex) { throw new IllegalStateException("Không đọc được tệp đính kèm", ex); }
    }

    private InboxMessage find(UUID id) {
        return jdbc.query(SELECT + " WHERE id = ?", (rs, n) -> map(rs), id).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Tin đến không tồn tại"));
    }
    private static InboxMessage map(ResultSet rs) throws SQLException {
        return new InboxMessage(rs.getObject("id", UUID.class), rs.getString("channel"),
                rs.getString("provider_message_id"), rs.getString("sender_address"),
                rs.getString("reply_destination"), rs.getString("recipient_address"),
                rs.getObject("sender_user_id", UUID.class),
                rs.getObject("matched_delivery_id", UUID.class), rs.getString("subject"),
                rs.getString("content"), rs.getString("kind"), rs.getString("handling_status"), time(rs, "received_at"));
    }
    private static Instant time(ResultSet rs, String field) throws SQLException {
        Timestamp value = rs.getTimestamp(field);
        return value == null ? null : value.toInstant();
    }
    private static String destination(InboxMessage message) {
        return message.replyDestination() == null || message.replyDestination().isBlank()
                ? message.senderAddress() : message.replyDestination();
    }
}
