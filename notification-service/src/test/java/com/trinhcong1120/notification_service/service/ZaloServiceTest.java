package com.trinhcong1120.notification_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class ZaloServiceTest {

    @Test
    void startUnlinkedSendsTwoSequentialMessagesAndSkipsDuplicate() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("account"), any(RowMapper.class), any()))
                .thenReturn(List.of());
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("zalo_chat_messages WHERE message_key"), any(RowMapper.class), eq("msg_001")))
                .thenReturn(List.of());

        RestClient.Builder builder = RestClient.builder().baseUrl("https://bot-api.zaloplatforms.com/bot_test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Expect Message 1: Welcome & Overview
        server.expect(requestTo("https://bot-api.zaloplatforms.com/bot_test/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("CHÀO MỪNG BẠN ĐẾN VỚI BOT KHẢO SÁT & BÁO CÁO ZALO"),
                        org.hamcrest.Matchers.containsString("Theo dõi số liệu thống kê")
                )))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":\"z1\"}}", MediaType.APPLICATION_JSON));

        // Expect Message 2: Instructions & Commands
        server.expect(requestTo("https://bot-api.zaloplatforms.com/bot_test/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("HƯỚNG DẪN BẮT ĐẦU SỬ DỤNG"),
                        org.hamcrest.Matchers.containsString("/link"),
                        org.hamcrest.Matchers.containsString("/thongke")
                )))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":\"z2\"}}", MediaType.APPLICATION_JSON));

        ZaloService zalo = new ZaloService("test", "", "", jdbc, builder.build());
        var update = new ObjectMapper().readTree("""
                {
                    "message": {
                        "message_id": "msg_001",
                        "chat": {"id": "zalo_123", "chat_type": "PRIVATE"},
                        "from": {"id": "zalo_123"},
                        "text": "/start"
                    }
                }
                """);
        zalo.handleUpdate(update);

        // Duplicate call with same message_key
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("zalo_chat_messages WHERE message_key"), any(RowMapper.class), eq("msg_001")))
                .thenReturn(List.of("SENT"));
        zalo.handleUpdate(update);

        server.verify();
    }

    @Test
    void followEventTriggersStartWelcomeTwoMessages() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("account"), any(RowMapper.class), any()))
                .thenReturn(List.of());
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("zalo_chat_messages WHERE message_key"), any(RowMapper.class), eq("ev_001")))
                .thenReturn(List.of());

        RestClient.Builder builder = RestClient.builder().baseUrl("https://bot-api.zaloplatforms.com/bot_test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Message 1
        server.expect(requestTo("https://bot-api.zaloplatforms.com/bot_test/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CHÀO MỪNG BẠN ĐẾN VỚI BOT KHẢO SÁT & BÁO CÁO ZALO")))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":\"ev_res_1\"}}", MediaType.APPLICATION_JSON));

        // Message 2
        server.expect(requestTo("https://bot-api.zaloplatforms.com/bot_test/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("HƯỚNG DẪN BẮT ĐẦU SỬ DỤNG")))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":\"ev_res_2\"}}", MediaType.APPLICATION_JSON));

        ZaloService zalo = new ZaloService("test", "", "", jdbc, builder.build());
        var update = new ObjectMapper().readTree("""
                {
                    "event_name": "follow",
                    "sender": {"id": "zalo_user_999"},
                    "msg_id": "ev_001",
                    "timestamp": 1600000000
                }
                """);
        zalo.handleUpdate(update);
        server.verify();
    }

    @Test
    void linkedAccountShowsPersonalizedMenu() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("zalo_chat_id"), any(RowMapper.class), eq("zalo_123"))).thenAnswer(call -> {
            RowMapper<?> mapper = call.getArgument(1);
            ResultSet row = mock(ResultSet.class);
            when(row.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
            when(row.getString("username")).thenReturn("congtrinh");
            when(row.getString("email")).thenReturn("trinh@example.com");
            return List.of(mapper.mapRow(row, 0));
        });
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("zalo_chat_messages WHERE message_key"), any(RowMapper.class), any()))
                .thenReturn(List.of());

        RestClient.Builder builder = RestClient.builder().baseUrl("https://bot-api.zaloplatforms.com/bot_test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Linked account gets a single personalized greeting & menu
        server.expect(requestTo("https://bot-api.zaloplatforms.com/bot_test/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Xin chào, congtrinh"),
                        org.hamcrest.Matchers.containsString("Chào mừng bạn quay lại hệ thống khảo sát"),
                        org.hamcrest.Matchers.containsString("/thongke"),
                        org.hamcrest.Matchers.containsString("/khaosat")
                )))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":\"z3\"}}", MediaType.APPLICATION_JSON));

        ZaloService zalo = new ZaloService("test", "", "", jdbc, builder.build());
        var update = new ObjectMapper().readTree("""
                {
                    "message": {
                        "message_id": "msg_002",
                        "chat": {"id": "zalo_123", "chat_type": "PRIVATE"},
                        "from": {"id": "zalo_123"},
                        "text": "/start"
                    }
                }
                """);
        zalo.handleUpdate(update);
        server.verify();
    }
}
