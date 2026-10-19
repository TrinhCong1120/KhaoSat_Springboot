package com.trinhcong1120.notification_service.service;

import jakarta.mail.Folder;
import jakarta.mail.FetchProfile;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.mail.Address;

@Service
public class EmailBounceMonitor {
    private static final Logger log = LoggerFactory.getLogger(EmailBounceMonitor.class);
    private static final Pattern FIELD = Pattern.compile("(?im)^%s:\\s*(.+)$");
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String folderName;
    private final String username;
    private final String password;
    private final InboxStore inbox;

    @Autowired
    public EmailBounceMonitor(JdbcTemplate jdbc,
            InboxStore inbox,
            @Value("${notification.bounce.enabled:false}") boolean enabled,
            @Value("${notification.bounce.host:imap.gmail.com}") String host,
            @Value("${notification.bounce.port:993}") int port,
            @Value("${notification.bounce.folder:INBOX}") String folderName,
            @Value("${spring.mail.username}") String username,
            @Value("${spring.mail.password}") String password) {
        this.jdbc = jdbc;
        this.inbox = inbox;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.folderName = folderName;
        this.username = username;
        this.password = password;
    }

    @Scheduled(fixedDelayString = "${notification.bounce.poll-delay-ms:60000}")
    public void poll() {
        if (!enabled) return;
        Properties properties = new Properties();
        properties.put("mail.imaps.connectiontimeout", "10000");
        properties.put("mail.imaps.timeout", "15000");
        properties.put("mail.imaps.writetimeout", "10000");
        Store store = null;
        Folder folder = null;
        try {
            store = Session.getInstance(properties).getStore("imaps");
            store.connect(host, port, username, password);
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            UIDFolder uids = (UIDFolder) folder;
            long validity = uids.getUIDValidity();
            long newest = uids.getUIDNext() - 1;
            List<Long> saved = jdbc.query("""
                SELECT last_uid FROM notification_bounce_cursor
                WHERE mailbox = ? AND uid_validity = ?
                """, (rs, n) -> rs.getLong(1), username + ":" + folderName, validity);
            // First activation starts at the current tip; existing bounce cursor is preserved.
            if (saved.isEmpty()) { checkpoint(validity, newest); return; }
            long first = saved.get(0) + 1;
            for (int batch = 0; first <= newest && batch < 100; batch++) {
                long last = Math.min(newest, first + 99);
                Message[] messages = uids.getMessagesByUID(first, last);
                FetchProfile profile = new FetchProfile();
                profile.add(FetchProfile.Item.CONTENT_INFO);
                folder.fetch(messages, profile);
                for (Message message : messages) {
                    saveIncoming(message, validity, uids.getUID(message));
                    Bounce bounce = parse(message);
                    if (bounce != null) record(bounce, message.getReceivedDate() == null
                            ? Instant.now() : message.getReceivedDate().toInstant());
                    checkpoint(validity, uids.getUID(message));
                }
                checkpoint(validity, last); // Also advance over unused UID numbers.
                first = last + 1;
            }
        } catch (Exception ex) {
            log.warn("Could not process email delivery reports: {}", ex.toString());
        } finally {
            if (folder != null && folder.isOpen()) {
                try { folder.close(false); } catch (MessagingException ex) { log.warn("Could not close IMAP folder", ex); }
            }
            if (store != null && store.isConnected()) {
                try { store.close(); } catch (MessagingException ex) { log.warn("Could not close IMAP store", ex); }
            }
        }
    }

    private void saveIncoming(Message message, long validity, long uid) throws Exception {
        Address[] from = message.getFrom();
        String sender = from != null && from.length > 0 && from[0] instanceof InternetAddress a
                ? a.getAddress() : "unknown";
        String recipient = username;
        Address[] replyAddresses = message.getReplyTo();
        String replyDestination = replyAddresses != null && replyAddresses.length == 1
                && replyAddresses[0] instanceof InternetAddress a ? a.getAddress() : sender;
        String[] ids = message.getHeader("Message-ID");
        String id = ids == null || ids.length == 0 ? null : ids[0];
        String[] reply = message.getHeader("In-Reply-To");
        String reference = reply != null && reply.length > 0 ? reply[0].trim() : null;
        if (reference == null) {
            String[] refs = message.getHeader("References");
            if (refs != null && refs.length > 0) {
                String[] tokens = refs[0].trim().split("\\s+");
                reference = tokens[tokens.length - 1];
            }
        }
        Bounce bounce = parse(message);
        String content = body(message);
        UUID inboxId = inbox.save("EMAIL", username + ":" + folderName + ":" + validity + ":" + uid,
                id, sender, recipient, message.getSubject(), content,
                bounce == null ? "MESSAGE" : "BOUNCE",
                message.getReceivedDate() == null ? Instant.now() : message.getReceivedDate().toInstant(),
                reference, replyDestination);
        attachments(message, inboxId, "0");
    }

    static String body(Part part) throws Exception {
        if (part.isMimeType("text/plain")) return String.valueOf(part.getContent());
        if (part.isMimeType("text/html")) return String.valueOf(part.getContent())
                .replaceAll("(?is)<(script|style).*?</\\1>", "")
                .replaceAll("(?i)<br\\s*/?>|</p>", "\n").replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        if (part.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) part.getContent();
            String html = "";
            for (int i = 0; i < multi.getCount(); i++) {
                Part child = multi.getBodyPart(i);
                if (child.getFileName() != null) continue;
                String result = body(child);
                if (child.isMimeType("text/plain") && !result.isBlank()) return result;
                if (html.isBlank()) html = result;
            }
            return html;
        }
        return "";
    }

    private void attachments(Part part, UUID inboxId, String path) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multi = (Multipart) part.getContent();
            for (int i = 0; i < multi.getCount(); i++) attachments(multi.getBodyPart(i), inboxId, path + "." + i);
        } else if (part.getFileName() != null) {
            long size = part.getSize();
            if (size > inbox.maxBytes()) {
                inbox.attachment(inboxId, path, part.getFileName(), part.getContentType(), size,
                        java.io.InputStream.nullInputStream());
            } else {
                try (var stream = part.getInputStream()) {
                    byte[] bytes = stream.readNBytes(Math.toIntExact(Math.min(Integer.MAX_VALUE, inbox.maxBytes() + 1)));
                    inbox.attachment(inboxId, path, part.getFileName(), part.getContentType(), bytes.length,
                            new java.io.ByteArrayInputStream(bytes));
                }
            }
        }
    }

    private void checkpoint(long validity, long uid) {
        jdbc.update("""
            INSERT INTO notification_bounce_cursor(mailbox, uid_validity, last_uid)
            VALUES (?, ?, ?)
            ON CONFLICT (mailbox) DO UPDATE SET
                last_uid = CASE WHEN notification_bounce_cursor.uid_validity = EXCLUDED.uid_validity
                                THEN greatest(notification_bounce_cursor.last_uid, EXCLUDED.last_uid)
                                ELSE EXCLUDED.last_uid END,
                uid_validity = EXCLUDED.uid_validity, updated_at = now()
            """, username + ":" + folderName, validity, uid);
    }

    private void record(Bounce bounce, Instant receivedAt) {
        UUID deliveryId = bounce.deliveryId();
        if (deliveryId == null && username.equalsIgnoreCase(bounce.originalSender())
                && bounce.originalSubject() != null) {
            List<UUID> matches = jdbc.query("""
                SELECT id FROM notification_deliveries
                WHERE channel = 'EMAIL' AND status = 'SENT'
                  AND lower(destination) = lower(?) AND subject = ?
                  AND sent_at BETWEEN ? AND ? LIMIT 2
                """, (rs, n) -> rs.getObject(1, UUID.class), bounce.recipient(),
                    bounce.originalSubject(), Timestamp.from(receivedAt.minusSeconds(604800)),
                    Timestamp.from(receivedAt.plusSeconds(300)));
            if (matches.size() != 1) return;
            deliveryId = matches.get(0);
        }
        if (deliveryId == null) return;
        int updated = jdbc.update("""
            UPDATE notification_deliveries
            SET status = 'BOUNCED', bounced_at = ?, error_note = ?
            WHERE id = ? AND channel = 'EMAIL' AND status = 'SENT'
              AND lower(destination) = lower(?)
            """, Timestamp.from(receivedAt), bounce.reason(), deliveryId, bounce.recipient());
        if (updated > 0) log.info("Recorded email bounce for delivery {}", deliveryId);
    }

    static Bounce parse(Message message) throws MessagingException, IOException {
        if (!message.isMimeType("multipart/report")) return null;
        Parts parts = new Parts();
        collect(message, parts);
        if (parts.report == null) return null;
        for (String block : parts.report.replaceAll("\\r?\\n[ \\t]+", " ").split("\\r?\\n\\s*\\r?\\n")) {
            if (!"failed".equalsIgnoreCase(field(block, "Action"))) continue;
            String recipient = field(block, "Final-Recipient");
            if (recipient == null) recipient = field(block, "Original-Recipient");
            if (recipient == null || !recipient.toLowerCase().startsWith("rfc822;")) continue;
            recipient = recipient.substring(recipient.indexOf(';') + 1).trim();
            String code = field(block, "Status");
            String diagnostic = field(block, "Diagnostic-Code");
            String reason = "Thư bị trả lại" + (code == null ? "" : " (" + code + ")")
                    + (diagnostic == null ? "" : ": " + diagnostic);
            return new Bounce(parts.deliveryId, recipient, reason.length() > 500
                    ? reason.substring(0, 500) : reason, parts.originalSubject, parts.originalSender);
        }
        return null;
    }

    private static void collect(Part part, Parts parts) throws MessagingException, IOException {
        if (part instanceof Message original) {
            String[] ids = original.getHeader("X-Notification-Delivery-Id");
            if (ids != null && ids.length == 1) {
                try { parts.deliveryId = UUID.fromString(ids[0].trim()); }
                catch (IllegalArgumentException ignored) { }
            }
            if (!original.isMimeType("multipart/report")
                    && parts.originalSubject == null && original.getSubject() != null
                    && original.getHeader("X-Notification-Delivery-Id") == null) {
                parts.originalSubject = original.getSubject();
                if (original.getFrom() != null && original.getFrom().length == 1
                        && original.getFrom()[0] instanceof InternetAddress sender)
                    parts.originalSender = sender.getAddress();
            }
        }
        if (part.isMimeType("message/delivery-status")) {
            parts.report = new String(part.getInputStream().readNBytes(65536), StandardCharsets.UTF_8);
        } else if (part.isMimeType("text/rfc822-headers")) {
            collect(new MimeMessage(Session.getInstance(new Properties()), part.getInputStream()), parts);
        } else if (part.isMimeType("message/rfc822")) {
            Object embedded = part.getContent();
            if (embedded instanceof Message original) collect(original, parts);
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) collect(multipart.getBodyPart(i), parts);
        }
    }

    private static String field(String block, String name) {
        Matcher match = Pattern.compile(FIELD.pattern().formatted(Pattern.quote(name)), FIELD.flags()).matcher(block);
        return match.find() ? match.group(1).trim() : null;
    }

    static record Bounce(UUID deliveryId, String recipient, String reason,
                         String originalSubject, String originalSender) { }
    private static final class Parts {
        UUID deliveryId;
        String report;
        String originalSubject;
        String originalSender;
    }
}
