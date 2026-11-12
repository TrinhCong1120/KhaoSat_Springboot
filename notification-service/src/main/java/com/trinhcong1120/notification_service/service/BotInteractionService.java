package com.trinhcong1120.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BotInteractionService {

    private static final Logger log = LoggerFactory.getLogger(BotInteractionService.class);

    private final JdbcTemplate jdbc;

    public BotInteractionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record BotAccount(UUID id, String username, String email, boolean isAdmin) {}

    public record UserStatsDto(
            int totalSurveys,
            int activeSurveys,
            long totalResponses,
            long todayResponses
    ) {}

    public record SurveySummaryDto(
            UUID id,
            String title,
            boolean isActive,
            long pageCount,
            long questionCount,
            long responseCount,
            Instant createdAt,
            String shareCode
    ) {}

    public record SurveyDetailDto(
            UUID id,
            String title,
            String description,
            boolean isActive,
            Instant openAt,
            Instant closeAt,
            Instant publishedAt,
            long pageCount,
            long questionCount,
            long responseCount,
            String shareCode,
            Instant createdAt
    ) {}

    public record AdminSystemStatsDto(
            long totalUsers,
            long totalSurveys,
            long activeSurveys,
            long totalResponses,
            long todayResponses,
            long pendingFailedCases
    ) {}

    public record FailedCaseSummaryDto(
            UUID id,
            String fileName,
            String originalFileName,
            long fileSize,
            String status,
            Instant createdAt
    ) {}

    /**
     * Tra cứu tài khoản theo telegram_chat_id hoặc zalo_chat_id kèm cờ isAdmin.
     */
    public BotAccount getAccountByChatId(String channel, String chatId) {
        String sql = "TELEGRAM".equalsIgnoreCase(channel)
                ? "SELECT id, username, email FROM users WHERE telegram_chat_id = ?"
                : "SELECT id, username, email FROM users WHERE zalo_chat_id = ?";

        Object param = "TELEGRAM".equalsIgnoreCase(channel) ? Long.parseLong(chatId) : chatId;

        List<BotAccount> accounts = jdbc.query(sql, (rs, n) -> {
            UUID userId = rs.getObject("id", UUID.class);
            String username = rs.getString("username");
            String email = rs.getString("email");
            boolean isAdmin = checkIsAdmin(userId);
            return new BotAccount(userId, username, email, isAdmin);
        }, param);

        return accounts.isEmpty() ? null : accounts.get(0);
    }

    /**
     * Kiểm tra user có Role ADMIN hoặc MANAGER hay không.
     */
    public boolean checkIsAdmin(UUID userId) {
        if (userId == null) return false;
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND lower(r.name) IN ('admin', 'manager')
                """, Integer.class, userId);
        return count != null && count > 0;
    }

    /**
     * Liên kết tài khoản tự động bằng mã OTP 6 số.
     */
    public String linkAccountByCode(String code, String channel, String chatId) {
        if (code == null || code.isBlank()) return null;
        String cleanCode = code.trim().toUpperCase();

        List<UUID> userIds = jdbc.query("""
                SELECT user_id FROM user_bot_link_codes
                WHERE code = ? AND channel = ? AND expires_at > now()
                """, (rs, n) -> rs.getObject("user_id", UUID.class), cleanCode, channel.toUpperCase());

        if (userIds.isEmpty()) return null;

        UUID targetUserId = userIds.get(0);

        if ("TELEGRAM".equalsIgnoreCase(channel)) {
            jdbc.update("UPDATE users SET telegram_chat_id = ? WHERE id = ?", Long.parseLong(chatId), targetUserId);
        } else {
            jdbc.update("UPDATE users SET zalo_chat_id = ? WHERE id = ?", chatId, targetUserId);
        }

        // Xóa mã đã sử dụng
        jdbc.update("DELETE FROM user_bot_link_codes WHERE code = ?", cleanCode);

        // Lấy lại username
        return jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, targetUserId);
    }

    /**
     * Hủy liên kết tài khoản khỏi chat ID hiện tại.
     */
    public boolean unlinkAccount(String channel, String chatId) {
        int updated;
        if ("TELEGRAM".equalsIgnoreCase(channel)) {
            updated = jdbc.update("UPDATE users SET telegram_chat_id = NULL WHERE telegram_chat_id = ?", Long.parseLong(chatId));
        } else {
            updated = jdbc.update("UPDATE users SET zalo_chat_id = NULL WHERE zalo_chat_id = ?", chatId);
        }
        return updated > 0;
    }

    /**
     * Lấy thống kê khảo sát và phản hồi của user (kèm phân quyền Admin).
     */
    public UserStatsDto getUserStats(UUID userId) {
        return getUserStats(userId, false);
    }

    public UserStatsDto getUserStats(UUID userId, boolean isAdmin) {
        if (isAdmin) {
            return jdbc.queryForObject("""
                    SELECT
                        COUNT(DISTINCT s.id) as total_surveys,
                        COUNT(DISTINCT CASE WHEN s.published_at IS NOT NULL AND s.manually_locked = false
                                            AND (s.close_at IS NULL OR s.close_at > now()) THEN s.id END) as active_surveys,
                        COALESCE(COUNT(r.id), 0) as total_responses,
                        COALESCE(COUNT(CASE WHEN r.submitted_at >= CURRENT_DATE THEN r.id END), 0) as today_responses
                    FROM surveys s
                    LEFT JOIN responses r ON r.survey_id = s.id
                    """, (rs, n) -> new UserStatsDto(
                    rs.getInt("total_surveys"),
                    rs.getInt("active_surveys"),
                    rs.getLong("total_responses"),
                    rs.getLong("today_responses")
            ));
        }
        return jdbc.queryForObject("""
                SELECT
                    COUNT(DISTINCT s.id) as total_surveys,
                    COUNT(DISTINCT CASE WHEN s.published_at IS NOT NULL AND s.manually_locked = false
                                        AND (s.close_at IS NULL OR s.close_at > now()) THEN s.id END) as active_surveys,
                    COALESCE(COUNT(r.id), 0) as total_responses,
                    COALESCE(COUNT(CASE WHEN r.submitted_at >= CURRENT_DATE THEN r.id END), 0) as today_responses
                FROM surveys s
                LEFT JOIN responses r ON r.survey_id = s.id
                WHERE s.creator_user_id = ?
                   OR EXISTS (SELECT 1 FROM survey_access_permissions sap WHERE sap.survey_id = s.id AND sap.user_id = ? AND sap.permission = 'VIEW')
                   OR EXISTS (SELECT 1 FROM survey_group_access sga WHERE sga.group_id = s.group_id AND sga.user_id = ? AND sga.permission = 'VIEW')
                """, (rs, n) -> new UserStatsDto(
                rs.getInt("total_surveys"),
                rs.getInt("active_surveys"),
                rs.getLong("total_responses"),
                rs.getLong("today_responses")
        ), userId, userId, userId);
    }

    /**
     * Đếm tổng số khảo sát mà user có quyền truy cập.
     */
    public int countUserSurveys(UUID userId) {
        return countUserSurveys(userId, false);
    }

    public int countUserSurveys(UUID userId, boolean isAdmin) {
        if (isAdmin) {
            Integer count = jdbc.queryForObject("SELECT COUNT(DISTINCT s.id) FROM surveys s", Integer.class);
            return count == null ? 0 : count;
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT s.id) FROM surveys s
                WHERE s.creator_user_id = ?
                   OR EXISTS (SELECT 1 FROM survey_access_permissions sap WHERE sap.survey_id = s.id AND sap.user_id = ? AND sap.permission = 'VIEW')
                   OR EXISTS (SELECT 1 FROM survey_group_access sga WHERE sga.group_id = s.group_id AND sga.user_id = ? AND sga.permission = 'VIEW')
                """, Integer.class, userId, userId, userId);
        return count == null ? 0 : count;
    }

    /**
     * Lấy danh sách khảo sát của user có phân trang (kèm phân quyền Admin).
     */
    public List<SurveySummaryDto> getUserSurveys(UUID userId, int page, int pageSize) {
        return getUserSurveys(userId, false, page, pageSize);
    }

    public List<SurveySummaryDto> getUserSurveys(UUID userId, boolean isAdmin, int page, int pageSize) {
        int offset = Math.max(0, page) * pageSize;
        String where = isAdmin
                ? ""
                : """
                  WHERE s.creator_user_id = ?
                     OR EXISTS (SELECT 1 FROM survey_access_permissions sap WHERE sap.survey_id = s.id AND sap.user_id = ? AND sap.permission = 'VIEW')
                     OR EXISTS (SELECT 1 FROM survey_group_access sga WHERE sga.group_id = s.group_id AND sga.user_id = ? AND sga.permission = 'VIEW')
                  """;

        Object[] params = isAdmin
                ? new Object[]{pageSize, offset}
                : new Object[]{userId, userId, userId, pageSize, offset};

        return jdbc.query("""
                SELECT s.id, s.title, s.share_code, s.created_at,
                       (s.published_at IS NOT NULL AND s.manually_locked = false AND (s.close_at IS NULL OR s.close_at > now())) as is_active,
                       (SELECT COUNT(p.id) FROM pages p WHERE p.survey_id = s.id) as page_count,
                       (SELECT COUNT(q.id) FROM questions q JOIN pages p ON q.page_id = p.id WHERE p.survey_id = s.id) as question_count,
                       (SELECT COUNT(r.id) FROM responses r WHERE r.survey_id = s.id) as response_count
                FROM surveys s
                """ + where + """
                ORDER BY s.created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, n) -> new SurveySummaryDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getBoolean("is_active"),
                rs.getLong("page_count"),
                rs.getLong("question_count"),
                rs.getLong("response_count"),
                toInstant(rs.getTimestamp("created_at")),
                rs.getString("share_code")
        ), params);
    }

    /**
     * Lấy thông tin chi tiết của 1 khảo sát theo UUID.
     */
    public SurveyDetailDto getSurveyDetail(UUID userId, UUID surveyId, boolean isAdmin) {
        String filter = isAdmin
                ? "WHERE s.id = ?"
                : """
                  WHERE s.id = ? AND (
                      s.creator_user_id = ?
                      OR EXISTS (SELECT 1 FROM survey_access_permissions sap WHERE sap.survey_id = s.id AND sap.user_id = ? AND sap.permission = 'VIEW')
                      OR EXISTS (SELECT 1 FROM survey_group_access sga WHERE sga.group_id = s.group_id AND sga.user_id = ? AND sga.permission = 'VIEW')
                  )
                  """;

        Object[] params = isAdmin ? new Object[]{surveyId} : new Object[]{surveyId, userId, userId, userId};

        List<SurveyDetailDto> list = jdbc.query("""
                SELECT s.id, s.title, s.description, s.share_code, s.created_at,
                       s.open_at, s.close_at, s.published_at,
                       (s.published_at IS NOT NULL AND s.manually_locked = false AND (s.close_at IS NULL OR s.close_at > now())) as is_active,
                       (SELECT COUNT(p.id) FROM pages p WHERE p.survey_id = s.id) as page_count,
                       (SELECT COUNT(q.id) FROM questions q JOIN pages p ON q.page_id = p.id WHERE p.survey_id = s.id) as question_count,
                       (SELECT COUNT(r.id) FROM responses r WHERE r.survey_id = s.id) as response_count
                FROM surveys s
                """ + filter, (rs, n) -> new SurveyDetailDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBoolean("is_active"),
                toInstant(rs.getTimestamp("open_at")),
                toInstant(rs.getTimestamp("close_at")),
                toInstant(rs.getTimestamp("published_at")),
                rs.getLong("page_count"),
                rs.getLong("question_count"),
                rs.getLong("response_count"),
                rs.getString("share_code"),
                toInstant(rs.getTimestamp("created_at"))
        ), params);

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Tra cứu khảo sát theo số thứ tự (1-based index).
     */
    public SurveyDetailDto getSurveyByIndex(UUID userId, int index, boolean isAdmin) {
        if (index < 0) return null;
        String where = isAdmin
                ? ""
                : """
                  WHERE s.creator_user_id = ?
                     OR EXISTS (SELECT 1 FROM survey_access_permissions sap WHERE sap.survey_id = s.id AND sap.user_id = ? AND sap.permission = 'VIEW')
                     OR EXISTS (SELECT 1 FROM survey_group_access sga WHERE sga.group_id = s.group_id AND sga.user_id = ? AND sga.permission = 'VIEW')
                  """;

        Object[] params = isAdmin ? new Object[]{1, index} : new Object[]{userId, userId, userId, 1, index};

        List<SurveyDetailDto> list = jdbc.query("""
                SELECT s.id, s.title, s.description, s.share_code, s.created_at,
                       s.open_at, s.close_at, s.published_at,
                       (s.published_at IS NOT NULL AND s.manually_locked = false AND (s.close_at IS NULL OR s.close_at > now())) as is_active,
                       (SELECT COUNT(p.id) FROM pages p WHERE p.survey_id = s.id) as page_count,
                       (SELECT COUNT(q.id) FROM questions q JOIN pages p ON q.page_id = p.id WHERE p.survey_id = s.id) as question_count,
                       (SELECT COUNT(r.id) FROM responses r WHERE r.survey_id = s.id) as response_count
                FROM surveys s
                """ + where + """
                ORDER BY s.created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, n) -> new SurveyDetailDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBoolean("is_active"),
                toInstant(rs.getTimestamp("open_at")),
                toInstant(rs.getTimestamp("close_at")),
                toInstant(rs.getTimestamp("published_at")),
                rs.getLong("page_count"),
                rs.getLong("question_count"),
                rs.getLong("response_count"),
                rs.getString("share_code"),
                toInstant(rs.getTimestamp("created_at"))
        ), params);

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Tra cứu khảo sát linh hoạt theo Số thứ tự (1, 2, 3...), ShareCode hoặc UUID.
     */
    public SurveyDetailDto getSurveyByCodeOrId(UUID userId, String codeOrId, boolean isAdmin) {
        if (codeOrId == null || codeOrId.isBlank()) return null;
        String val = codeOrId.trim();

        // 1. Kiểm tra nếu là số thứ tự 1-1000
        try {
            int num = Integer.parseInt(val);
            if (num >= 1 && num <= 1000) {
                SurveyDetailDto byIndex = getSurveyByIndex(userId, num - 1, isAdmin);
                if (byIndex != null) return byIndex;
            }
        } catch (NumberFormatException ignored) {}

        // 2. Kiểm tra nếu là UUID
        try {
            UUID id = UUID.fromString(val);
            return getSurveyDetail(userId, id, isAdmin);
        } catch (Exception ignored) {}

        // 3. Tra cứu theo share_code
        String filter = isAdmin
                ? "WHERE lower(s.share_code) = lower(?)"
                : """
                  WHERE lower(s.share_code) = lower(?) AND (
                      s.creator_user_id = ?
                      OR EXISTS (SELECT 1 FROM survey_access_permissions sap WHERE sap.survey_id = s.id AND sap.user_id = ? AND sap.permission = 'VIEW')
                      OR EXISTS (SELECT 1 FROM survey_group_access sga WHERE sga.group_id = s.group_id AND sga.user_id = ? AND sga.permission = 'VIEW')
                  )
                  """;

        Object[] params = isAdmin ? new Object[]{val} : new Object[]{val, userId, userId, userId};

        List<SurveyDetailDto> list = jdbc.query("""
                SELECT s.id, s.title, s.description, s.share_code, s.created_at,
                       s.open_at, s.close_at, s.published_at,
                       (s.published_at IS NOT NULL AND s.manually_locked = false AND (s.close_at IS NULL OR s.close_at > now())) as is_active,
                       (SELECT COUNT(p.id) FROM pages p WHERE p.survey_id = s.id) as page_count,
                       (SELECT COUNT(q.id) FROM questions q JOIN pages p ON q.page_id = p.id WHERE p.survey_id = s.id) as question_count,
                       (SELECT COUNT(r.id) FROM responses r WHERE r.survey_id = s.id) as response_count
                FROM surveys s
                """ + filter, (rs, n) -> new SurveyDetailDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBoolean("is_active"),
                toInstant(rs.getTimestamp("open_at")),
                toInstant(rs.getTimestamp("close_at")),
                toInstant(rs.getTimestamp("published_at")),
                rs.getLong("page_count"),
                rs.getLong("question_count"),
                rs.getLong("response_count"),
                rs.getString("share_code"),
                toInstant(rs.getTimestamp("created_at"))
        ), params);

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Báo cáo tổng thể toàn hệ thống cho Admin / Manager.
     */
    public AdminSystemStatsDto getAdminSystemStats() {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM users) as total_users,
                    (SELECT COUNT(*) FROM surveys) as total_surveys,
                    (SELECT COUNT(*) FROM surveys WHERE published_at IS NOT NULL AND manually_locked = false
                                                    AND (close_at IS NULL OR close_at > now())) as active_surveys,
                    (SELECT COUNT(*) FROM responses) as total_responses,
                    (SELECT COUNT(*) FROM responses WHERE submitted_at >= CURRENT_DATE) as today_responses,
                    (SELECT COUNT(*) FROM failed_survey_cases WHERE status = 'NEW') as pending_failed_cases
                """, (rs, n) -> new AdminSystemStatsDto(
                rs.getLong("total_users"),
                rs.getLong("total_surveys"),
                rs.getLong("active_surveys"),
                rs.getLong("total_responses"),
                rs.getLong("today_responses"),
                rs.getLong("pending_failed_cases")
        ));
    }

    /**
     * Danh sách bài nộp khảo sát bị lỗi cho Admin.
     */
    public List<FailedCaseSummaryDto> getRecentFailedCases(int limit) {
        return jdbc.query("""
                SELECT id, file_name, original_file_name, file_size, status, created_at
                FROM failed_survey_cases
                ORDER BY created_at DESC
                LIMIT ?
                """, (rs, n) -> new FailedCaseSummaryDto(
                rs.getObject("id", UUID.class),
                rs.getString("file_name"),
                rs.getString("original_file_name"),
                rs.getLong("file_size"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("created_at"))
        ), limit);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
