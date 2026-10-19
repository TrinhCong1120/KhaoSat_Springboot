package com.trinhcong1120.notification_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.UUID;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class TelegramServiceTest {
    @Test
    void freeTextIsRecordedWithoutAutomaticResponse() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InboxStore inbox = mock(InboxStore.class);
        TelegramService telegram = new TelegramService("test", "", "", jdbc, RestClient.create());
        telegram.setInbox(inbox);
        var update = new ObjectMapper().readTree("""
            {"update_id":91,"message":{"message_id":42,"date":100,
             "chat":{"id":123,"type":"private"},"from":{"id":123},
             "text":"Xin chào","reply_to_message":{"message_id":12}}}
            """);
        telegram.handleUpdate(update);
        verify(inbox).save(eq("TELEGRAM"), eq("telegram:91"), eq("42"), eq("123"),
                eq("bot"), eq(null), eq("Xin chào"), eq("MESSAGE"), any(), eq("12"));
        verifyNoInteractions(jdbc);
    }
    @Test
    void startShowsTwoButtonsAndDuplicateUpdateDoesNotSendAgain() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(123L))).thenReturn(List.of());
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1, 1, 0);
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org/bottest");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.telegram.org/bottest/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Kiểm tra thông tin"),
                        org.hamcrest.Matchers.containsString("Đăng ký thông tin"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Lấy Chat ID")))))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
        TelegramService telegram = new TelegramService("test", "", "", jdbc, builder.build());
        var update = new ObjectMapper().readTree("""
                {"update_id":7,"message":{"chat":{"id":123,"type":"private"},
                "from":{"id":123},"text":"/start"}}
                """);
        telegram.handleUpdate(update);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(7L))).thenReturn(List.of("SENT"));
        telegram.handleUpdate(update);
        server.verify();
    }

    @Test
    void linkedAccountShowsMaskedEmailAndGetIdStillWorks() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(123L))).thenAnswer(call -> {
            RowMapper<?> mapper = call.getArgument(1);
            ResultSet row = mock(ResultSet.class);
            when(row.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
            when(row.getString("username")).thenReturn("demo");
            when(row.getString("email")).thenReturn("demo@example.com");
            return List.of(mapper.mapRow(row, 0));
        });
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org/bottest");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.telegram.org/bottest/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("demo"),
                        org.hamcrest.Matchers.containsString("d***@example.com"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("demo@example.com")))))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.telegram.org/bottest/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("123")))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
        TelegramService telegram = new TelegramService("test", "", "", jdbc, builder.build());
        ObjectMapper mapper = new ObjectMapper();
        telegram.handleUpdate(mapper.readTree("""
                {"update_id":8,"message":{"chat":{"id":123,"type":"private"},
                "from":{"id":123},"text":"/thongtin"}}
                """));
        telegram.handleUpdate(mapper.readTree("""
                {"update_id":9,"message":{"chat":{"id":123,"type":"private"},
                "from":{"id":123},"text":"/getID"}}
                """));
        server.verify();
    }
}
