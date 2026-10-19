package com.trinhcong1120.notification_service.service;

import static org.junit.jupiter.api.Assertions.*;
import javax.naming.directory.BasicAttribute;
import org.junit.jupiter.api.Test;

class EmailServiceTest {
    @Test
    void rejectsOnlyExplicitNullMx() throws Exception {
        assertTrue(EmailService.hasNullMx(new BasicAttribute("MX", "0 .")));
        assertFalse(EmailService.hasNullMx(new BasicAttribute("MX", "10 mail.example.org.")));
        assertFalse(EmailService.hasNullMx(null));
        BasicAttribute multiple = new BasicAttribute("MX", "0 .");
        multiple.add("10 mail.example.org.");
        assertFalse(EmailService.hasNullMx(multiple));
    }
}
