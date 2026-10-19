package com.trinhcong1120.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    public String sendEmail(UUID deliveryId,
            String to,
            String subject,
            String content
    ) {
        return sendEmail(deliveryId, to, subject, content, null);
    }

    public String sendEmail(UUID deliveryId, String to, String subject, String content, String inReplyTo) {
        validateRecipientDomain(to);

        try {
            MimeMessage mail = mailSender.createMimeMessage();
            mail.setFrom(new InternetAddress(fromEmail));
            mail.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(to, true));
            mail.setSubject(subject, "UTF-8");
            mail.setText(content, "UTF-8");
            mail.setHeader("X-Notification-Delivery-Id", deliveryId.toString());
            if (inReplyTo != null && inReplyTo.matches("<[^<>\\r\\n]{1,490}>")) {
                mail.setHeader("In-Reply-To", inReplyTo);
                mail.setHeader("References", inReplyTo);
            }
            mail.saveChanges();
            mailSender.send(mail);
            log.info("SMTP accepted email for {}", to);
            return mail.getMessageID();
        } catch (MessagingException ex) {
            throw new IllegalArgumentException("Không tạo được email gửi đi", ex);
        }

    }

    private void validateRecipientDomain(String to) {
        final String domain;
        try {
            InternetAddress address = new InternetAddress(to, true);
            address.validate();
            if (!address.getAddress().equals(to)) throw new AddressException("Expected one email address");
            domain = to.substring(to.lastIndexOf('@') + 1);
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ: " + to, ex);
        }

        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        environment.put("com.sun.jndi.dns.timeout.initial", "2000");
        environment.put("com.sun.jndi.dns.timeout.retries", "1");
        DirContext dns = null;
        try {
            dns = new InitialDirContext(environment);
            Attributes attributes = dns.getAttributes(domain, new String[]{"MX"});
            if (hasNullMx(attributes.get("MX"))) {
                throw new IllegalArgumentException("Tên miền " + domain + " không nhận email (Null MX)");
            }
        } catch (NameNotFoundException ex) {
            throw new IllegalArgumentException("Tên miền email không tồn tại: " + domain, ex);
        } catch (NamingException ex) {
            throw new RuntimeException("Không kiểm tra được DNS cho tên miền " + domain, ex);
        } finally {
            if (dns != null) {
                try { dns.close(); } catch (NamingException ignored) { }
            }
        }
    }

    static boolean hasNullMx(Attribute mx) throws NamingException {
        return mx != null && mx.size() == 1
                && "0 .".equals(mx.get().toString().trim().replaceAll("\\s+", " "));
    }
}
