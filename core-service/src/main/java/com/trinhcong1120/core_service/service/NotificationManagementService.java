package com.trinhcong1120.core_service.service;

import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.exception.NotFoundException;
import com.trinhcong1120.core_service.repository.UserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationManagementService {
    public record CreateRequest(List<UUID> recipientIds, List<String> channels,
                                String subject, String content) {}
    public record Created(List<UUID> ids) {}
    public record Delivery(UUID id, String source, UUID actorUserId, String actorName,
                           UUID recipientUserId, String recipientName, String destination,
                           String channel, String subject, String content, String eventType,
                           UUID referenceId, UUID retryOf, String status, int attempts,
                           Instant createdAt, Instant nextAttemptAt, Instant sentAt, Instant bouncedAt,
                           String errorNote, String providerMessageId, UUID inReplyToInboundId) {}
    public record Attempt(int attemptNo, Instant startedAt, Instant finishedAt,
                          String status, String errorNote) {}
    public record Recipient(UUID id, String username, String email, Long telegramChatId) {}
    public record Detail(Delivery delivery, List<Attempt> attempts) {}
    public record Page(List<Delivery> content, int page, int size, long totalElements,
                       int totalPages) {}
    public record ChatMessage(long id, long chatId, UUID accountUserId, String accountName,
                              String direction, String action, String content, String status,
                              String errorNote, Instant createdAt) {}
    public record ChatPage(List<ChatMessage> content, int page, int size, long totalElements,
                           int totalPages) {}

    private static final String SELECT = """
        SELECT id, source, actor_user_id, actor_name, recipient_user_id, recipient_name,
               destination, channel, subject, content, event_type, reference_id,
               retry_of, status, attempts, created_at, next_attempt_at, sent_at, bounced_at, error_note,
               provider_message_id, in_reply_to_inbound_id
        FROM notification_deliveries
        """;
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public NotificationManagementService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ChatPage chats(int page, Long chatId) {
        if (page < 1 || page > 100000) throw new BadRequestException("Trang không hợp lệ");
        int size = 20;
        String where = chatId == null ? "" : " WHERE m.chat_id = ?";
        Object[] filter = chatId == null ? new Object[0] : new Object[]{chatId};
        Long total = jdbc.queryForObject("SELECT count(*) FROM telegram_chat_messages m" + where,
                Long.class, filter);
        List<Object> args = new ArrayList<>(List.of(filter));
        args.add(size);
        args.add((page - 1) * size);
        List<ChatMessage> content = jdbc.query("""
                SELECT m.id, m.chat_id, m.account_user_id, u.username AS account_name,
                       m.direction, m.action, m.content, m.status, m.error_note, m.created_at
                FROM telegram_chat_messages m LEFT JOIN users u ON u.id = m.account_user_id
                """ + where + " ORDER BY m.created_at DESC, m.id DESC LIMIT ? OFFSET ?",
                (rs, n) -> new ChatMessage(rs.getLong("id"), rs.getLong("chat_id"),
                        rs.getObject("account_user_id", UUID.class), rs.getString("account_name"),
                        rs.getString("direction"), rs.getString("action"), rs.getString("content"),
                        rs.getString("status"), rs.getString("error_note"), time(rs, "created_at")),
                args.toArray());
        long count = total == null ? 0 : total;
        return new ChatPage(content, page, size, count, (int) ((count + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public Page list(int page, String status, String channel, String q, Instant from, Instant to) {
        if (page < 1 || page > 100000) throw new BadRequestException("Trang không hợp lệ");
        if (from != null && to != null && from.isAfter(to))
            throw new BadRequestException("Khoảng thời gian không hợp lệ");
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            String value = status.trim().toUpperCase();
            if (!Set.of("PENDING", "PROCESSING", "SENT", "FAILED", "BOUNCED").contains(value))
                throw new BadRequestException("Trạng thái không hợp lệ");
            where.append(" AND status = ?"); args.add(value);
        }
        if (channel != null && !channel.isBlank()) {
            String value = channel.trim().toUpperCase();
            if (!Set.of("EMAIL", "TELEGRAM").contains(value))
                throw new BadRequestException("Kênh không hợp lệ");
            where.append(" AND channel = ?"); args.add(value);
        }
        if (q != null && !q.isBlank()) {
            if (q.length() > 100) throw new BadRequestException("Từ khóa quá dài");
            where.append(" AND (recipient_name ILIKE ? OR destination ILIKE ? OR actor_name ILIKE ?)");
            String search = "%" + q.trim().replace("%", "\\%").replace("_", "\\_") + "%";
            args.add(search); args.add(search); args.add(search);
        }
        if (from != null) { where.append(" AND created_at >= ?"); args.add(Timestamp.from(from)); }
        if (to != null) { where.append(" AND created_at <= ?"); args.add(Timestamp.from(to)); }
        Long count = jdbc.queryForObject("SELECT count(*) FROM notification_deliveries" + where,
                Long.class, args.toArray());
        int size = 20;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((page - 1) * size);
        List<Delivery> rows = jdbc.query(SELECT + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, n) -> delivery(rs), pageArgs.toArray());
        long total = count == null ? 0 : count;
        return new Page(rows, page, size, total, (int) ((total + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public Detail detail(UUID id) {
        Delivery row = find(id);
        List<Attempt> attempts = jdbc.query("""
                SELECT attempt_no, started_at, finished_at, status, error_note
                FROM notification_attempts WHERE delivery_id = ? ORDER BY attempt_no
                """, (rs, n) -> new Attempt(rs.getInt("attempt_no"), time(rs, "started_at"),
                time(rs, "finished_at"), rs.getString("status"), rs.getString("error_note")), id);
        return new Detail(row, attempts);
    }

    @Transactional(readOnly = true)
    public List<Recipient> recipients(String q) {
        String search = q == null ? "" : q.trim();
        if (search.length() > 100) throw new BadRequestException("Từ khóa quá dài");
        return jdbc.query("""
            SELECT id, username, email, telegram_chat_id FROM users
            WHERE is_active IS TRUE
              AND (username ILIKE ? OR email ILIKE ?)
            ORDER BY lower(username), id LIMIT 30
            """, (rs, n) -> new Recipient(rs.getObject("id", UUID.class),
                rs.getString("username"), rs.getString("email"),
                rs.getObject("telegram_chat_id", Long.class)), "%" + search + "%", "%" + search + "%");
    }

    @Transactional
    public Created create(CreateRequest request, UUID actorId) {
        if (request == null || request.recipientIds() == null || request.channels() == null
                || request.subject() == null || request.content() == null)
            throw new BadRequestException("Thiếu thông tin gửi thông báo");
        Set<UUID> recipientIds = new LinkedHashSet<>(request.recipientIds());
        Set<String> channels = new LinkedHashSet<>(request.channels());
        if (recipientIds.isEmpty() || recipientIds.size() > 100 || recipientIds.contains(null))
            throw new BadRequestException("Chọn từ 1 đến 100 người nhận");
        if (channels.isEmpty() || channels.stream().anyMatch(c -> c == null
                || !Set.of("EMAIL", "TELEGRAM").contains(c)))
            throw new BadRequestException("Kênh gửi không hợp lệ");
        String subject = request.subject().trim();
        String content = request.content().trim();
        if (subject.isEmpty() || subject.length() > 500 || content.isEmpty() || content.length() > 10000)
            throw new BadRequestException("Tiêu đề hoặc nội dung không hợp lệ");
        User actor = users.findByIdAndIsActiveTrue(actorId).orElseThrow(() -> new NotFoundException("Người gửi không tồn tại"));
        List<User> recipients = users.findAllById(recipientIds);
        if (recipients.size() != recipientIds.size()) throw new BadRequestException("Có người nhận không tồn tại");
        List<UUID> ids = new ArrayList<>();
        for (User recipient : recipients) {
            if (!Boolean.TRUE.equals(recipient.getIsActive()))
                throw new BadRequestException("Người nhận không hoạt động: " + recipient.getUsername());
            for (String channel : channels) {
                String destination = channel.equals("EMAIL") ? recipient.getEmail()
                        : recipient.getTelegramChatId() == null ? null : recipient.getTelegramChatId().toString();
                if (destination == null || destination.isBlank())
                    throw new BadRequestException(recipient.getUsername() + " chưa liên kết " + channel);
                UUID id = UUID.randomUUID();
                jdbc.update("""
                    INSERT INTO notification_deliveries
                    (id, source, actor_user_id, actor_name, recipient_user_id, recipient_name,
                     destination, channel, subject, content)
                    VALUES (?, 'MANUAL', ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, actorId, actor.getUsername(), recipient.getId(), recipient.getUsername(),
                        destination.trim(), channel, subject, content);
                ids.add(id);
            }
        }
        return new Created(ids);
    }

    @Transactional
    public Created retry(UUID id, UUID actorId) {
        List<Delivery> rows = jdbc.query(SELECT + " WHERE id = ? FOR UPDATE", (rs, n) -> delivery(rs), id);
        if (rows.isEmpty()) throw new NotFoundException("Thông báo không tồn tại");
        Delivery old = rows.get(0);
        if (!Set.of("FAILED", "BOUNCED").contains(old.status())) throw new BadRequestException("Chỉ gửi lại thông báo thất bại");
        Long active = jdbc.queryForObject("""
            SELECT count(*) FROM notification_deliveries
            WHERE retry_of = ? AND status NOT IN ('FAILED', 'BOUNCED')
            """, Long.class, id);
        if (active != null && active > 0) throw new BadRequestException("Thông báo đã được gửi lại");
        User actor = users.findByIdAndIsActiveTrue(actorId).orElseThrow(() -> new NotFoundException("Người gửi không tồn tại"));
        UUID newId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO notification_deliveries
            (id, source, actor_user_id, actor_name, recipient_user_id, recipient_name,
             destination, channel, subject, content, event_type, reference_id, retry_of, in_reply_to_inbound_id)
            VALUES (?, 'MANUAL', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                (SELECT in_reply_to_inbound_id FROM notification_deliveries WHERE id = ?))
            """, newId, actorId, actor.getUsername(), old.recipientUserId(), old.recipientName(),
                old.destination(), old.channel(), old.subject(), old.content(), old.eventType(),
                old.referenceId(), id, id);
        return new Created(List.of(newId));
    }

    private Delivery find(UUID id) {
        List<Delivery> rows = jdbc.query(SELECT + " WHERE id = ?", (rs, n) -> delivery(rs), id);
        if (rows.isEmpty()) throw new NotFoundException("Thông báo không tồn tại");
        return rows.get(0);
    }

    private static Delivery delivery(ResultSet rs) throws SQLException {
        return new Delivery(rs.getObject("id", UUID.class), rs.getString("source"),
                rs.getObject("actor_user_id", UUID.class), rs.getString("actor_name"),
                rs.getObject("recipient_user_id", UUID.class), rs.getString("recipient_name"),
                rs.getString("destination"), rs.getString("channel"), rs.getString("subject"),
                rs.getString("content"), rs.getString("event_type"),
                rs.getObject("reference_id", UUID.class), rs.getObject("retry_of", UUID.class),
                rs.getString("status"), rs.getInt("attempts"), time(rs, "created_at"),
                time(rs, "next_attempt_at"), time(rs, "sent_at"), time(rs, "bounced_at"), rs.getString("error_note"),
                rs.getString("provider_message_id"), rs.getObject("in_reply_to_inbound_id", UUID.class));
    }

    private static Instant time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
