package com.trinhcong1120.notification_service.consumer;

import com.trinhcong1120.notification_service.dto.NotificationEvent;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final JdbcTemplate jdbc;

    public NotificationConsumer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @KafkaListener(topics = "notification.events", groupId = "notification-service")
    public void consume(ConsumerRecord<String, NotificationEvent> record) {
        NotificationEvent event = record.value();
        if (event == null) return;
        boolean email = event.getEmail() != null && !event.getEmail().isBlank();
        if (!email && event.getTelegramChatId() == null) {
            log.warn("Ignoring notification event {} without a destination", event.getEventType());
            return;
        }
        String channel = email ? "EMAIL" : "TELEGRAM";
        String destination = email ? event.getEmail().trim() : event.getTelegramChatId().toString();
        UUID recipientId = event.getRecipientId();
        String recipientName = event.getUsername();
        if (recipientId == null) {
            String lookup = email ? "SELECT id, username FROM users WHERE lower(email) = lower(?)"
                    : "SELECT id, username FROM users WHERE telegram_chat_id = ?";
            List<UserInfo> found = jdbc.query(lookup,
                    (rs, n) -> new UserInfo(rs.getObject("id", UUID.class), rs.getString("username")),
                    email ? destination : event.getTelegramChatId());
            if (found.size() == 1) {
                recipientId = found.get(0).id();
                recipientName = found.get(0).username();
            }
        }
        String actorName = null;
        if (event.getActorId() != null) {
            List<String> names = jdbc.query("SELECT username FROM users WHERE id = ?",
                    (rs, n) -> rs.getString(1), event.getActorId());
            actorName = names.isEmpty() ? null : names.get(0);
        }
        String originKey = event.getEventId() == null
                ? record.topic() + ":" + record.partition() + ":" + record.offset()
                : "event:" + event.getEventId();
        jdbc.update("""
            INSERT INTO notification_deliveries
            (origin_key, source, actor_user_id, actor_name, recipient_user_id, recipient_name,
             destination, channel, subject, content, event_type, reference_id)
            VALUES (?, 'SYSTEM', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (origin_key) DO NOTHING
            """, originKey, event.getActorId(), actorName, recipientId, recipientName,
                destination, channel, event.getSubject() == null ? "" : event.getSubject(),
                event.getMessage() == null ? "" : event.getMessage(), event.getEventType(),
                event.getReferenceId());
    }

    private record UserInfo(UUID id, String username) {}
}
