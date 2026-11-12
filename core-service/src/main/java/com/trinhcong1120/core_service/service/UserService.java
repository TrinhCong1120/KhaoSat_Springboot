package com.trinhcong1120.core_service.service;

import java.util.UUID;

import com.trinhcong1120.core_service.dto.event.NotificationEvent;
import com.trinhcong1120.core_service.dto.user.CreateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateProfileRequest;
import com.trinhcong1120.core_service.dto.user.ChangePasswordRequest;
import com.trinhcong1120.core_service.dto.user.UserSearchResponse;
import com.trinhcong1120.core_service.dto.user.UserResponse;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.producer.NotificationProducer;
import com.trinhcong1120.core_service.repository.RoleRepository;
import com.trinhcong1120.core_service.repository.UserRepository;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.dto.common.PagedResponse;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
public class UserService {

    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer notificationProducer;
    private final AdminGrantAlertService adminGrantAlertService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            NotificationProducer notificationProducer,
            AdminGrantAlertService adminGrantAlertService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationProducer = notificationProducer;
        this.adminGrantAlertService = adminGrantAlertService;
    }

    // =========================
    // GET ALL USERS
    // =========================

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsersPage(int page) {
        validatePage(page);
        return PagedResponse.from(userRepository
                .findAll(PageRequest.of(page - 1, PAGE_SIZE,
                        Sort.by(Sort.Order.asc("username"), Sort.Order.asc("id"))))
                .map(this::toUserResponse));
    }

    private void validatePage(int page) {
        if (page < 1) throw new BadRequestException("page phai bat dau tu 1");
    }

    @Transactional(readOnly = true)
    public UserResponse getActiveUser(UUID id) {

        User user =
                userRepository
                        .findByIdAndIsActiveTrue(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User khong ton tai hoac da bi khoa"
                                )
                        );

        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchActiveUsers(String query) {
        if (query == null || query.isBlank() || query.trim().length() > 255) {
            throw new BadRequestException("q phai co tu 1 den 255 ky tu");
        }

        return userRepository.searchActiveByExactUsernameOrEmail(
                        query.trim().toLowerCase(Locale.ROOT), PageRequest.of(0, 20))
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(), user.getUsername(), maskEmail(user.getEmail())))
                .toList();
    }

    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return null;
        }
        int visible = Math.min(4, at - 1);
        return email.substring(0, visible) + "****" + email.substring(at);
    }

    // =========================
    // CREATE USER
    // =========================

    @Transactional
    public UUID createUser(CreateUserRequest request, UUID actorId) {

        if (request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Vui lòng nhập Username và Password"
            );
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username đã tồn tại"
            );
        }

        User user = new User();

        user.setUsername(
                request.getUsername().trim()
        );

        user.setEmail(
                normalizeEmail(request.getEmail())
        );

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setIsActive(true);
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        validateTelegramChatId(request.getTelegramChatId(), null);
        user.setTelegramChatId(request.getTelegramChatId());
        validateZaloChatId(request.getZaloChatId(), null);
        user.setZaloChatId(normalizeZaloChatId(request.getZaloChatId()));

        // =========================
        // GÁN ROLE
        // =========================

        if (request.getRoleIds() != null
                && !request.getRoleIds().isEmpty()) {

            List<Role> roles =
                    roleRepository.findAllById(
                            request.getRoleIds()
                    );

            user.setRoles(
                    new HashSet<>(roles)
            );
        }

        // =========================
        // LƯU USER
        // =========================

        User savedUser =
                userRepository.save(user);

        if (hasAdminRole(savedUser)) {
            adminGrantAlertService.notifyGrant(savedUser, actorId);
        }

        if (savedUser.getTelegramChatId() != null) {
            publishTelegramLinked(savedUser, actorId);
        }
        if (savedUser.getZaloChatId() != null) {
            publishZaloLinked(savedUser, actorId);
        }

        // =========================
        // GỬI KAFKA EVENT
        // =========================

        publishUserNotification(savedUser, actorId, "USER_CREATED",
                "Tài khoản đã được tạo",
                "Xin chào " + savedUser.getUsername()
                        + ", tài khoản của bạn đã được tạo thành công.");

        return savedUser.getId();
    }

    // =========================
    // UPDATE USER
    // =========================

    @Transactional
    public void updateUser(
            UUID id,
            UpdateUserRequest request,
            UUID actorId
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User không tồn tại"
                        )
                );

        /*
         * Lưu trạng thái cũ để biết user
         * có vừa bị khóa hay không.
         */
        boolean wasActive =
                Boolean.TRUE.equals(
                        user.getIsActive()
                );
        boolean hadAdminRole = hasAdminRole(user);
        Long oldTelegramChatId = user.getTelegramChatId();
        String oldZaloChatId = user.getZaloChatId();
        Set<String> oldRoles = roleNames(user);

        // =========================
        // UPDATE EMAIL
        // =========================

        if (request.getEmail() != null) {

            user.setEmail(
                    normalizeEmail(
                            request.getEmail()
                    )
            );
        }

        // =========================
        // UPDATE PASSWORD
        // =========================

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            user.setPasswordHash(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }

        // =========================
        // UPDATE ROLE
        // =========================

        /*
         * roleIds == null
         * => giữ nguyên role
         *
         * roleIds == []
         * => xóa toàn bộ role
         *
         * roleIds == [1,2]
         * => thay bằng role 1,2
         */
        if (request.getRoleIds() != null) {

            List<Role> roles =
                    roleRepository.findAllById(
                            request.getRoleIds()
                    );

            user.setRoles(
                    new HashSet<>(roles)
            );
        }

        // =========================
        // UPDATE STATUS
        // =========================

        if (request.getIsActive() != null) {

            user.setIsActive(
                    request.getIsActive()
            );
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.isTelegramChatIdPresent()) {
            validateTelegramChatId(request.getTelegramChatId(), id);
            user.setTelegramChatId(request.getTelegramChatId());
        }
        if (request.isZaloChatIdPresent()) {
            validateZaloChatId(request.getZaloChatId(), id);
            user.setZaloChatId(normalizeZaloChatId(request.getZaloChatId()));
        }

        // =========================
        // SAVE
        // =========================

        User savedUser =
                userRepository.save(user);

        if (!hadAdminRole && hasAdminRole(savedUser)) {
            adminGrantAlertService.notifyGrant(savedUser, actorId);
        }

        if (savedUser.getTelegramChatId() != null
                && !savedUser.getTelegramChatId().equals(oldTelegramChatId)) {
            publishTelegramLinked(savedUser, actorId);
        }
        if (savedUser.getZaloChatId() != null
                && !savedUser.getZaloChatId().equals(oldZaloChatId)) {
            publishZaloLinked(savedUser, actorId);
        }

        Set<String> newRoles = roleNames(savedUser);
        if (!oldRoles.equals(newRoles)
                && (savedUser.getTelegramChatId() != null || savedUser.getZaloChatId() != null)) {
            publishRoleChanges(savedUser, actorId, oldRoles, newRoles);
        }

        // =========================
        // KIỂM TRA USER VỪA BỊ KHÓA
        // =========================

        boolean isNowInactive =
                Boolean.FALSE.equals(
                        savedUser.getIsActive()
                );

        /*
         * Chỉ gửi mail khi:
         *
         * true -> false
         *
         * Không gửi lại nếu user vốn
         * đã bị khóa từ trước.
         */
        if (wasActive && isNowInactive) {
            publishUserNotification(savedUser, actorId, "USER_DEACTIVATED",
                    "Tài khoản đã bị khóa",
                    "Xin chào " + savedUser.getUsername()
                            + ", tài khoản của bạn đã bị khóa.");
        }
    }

    // =========================
    // DELETE USER
    // =========================

    @Transactional
    public void deleteUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User không tồn tại"
                        )
                );

        userRepository.delete(user);
    }

    // =========================
    // ENTITY -> DTO
    // =========================

    private UserResponse toUserResponse(
            User user
    ) {

        List<String> roles =
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getIsActive(),
                roles,
                user.getTelegramChatId(),
                user.getZaloChatId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getBotNotificationEnabled()
        );
    }

    // =========================
    // NORMALIZE EMAIL
    // =========================

    private String normalizeEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            return null;
        }

        return email.trim();
    }

    private boolean hasAdminRole(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "admin".equalsIgnoreCase(role.getName()));
    }

    private void validateTelegramChatId(Long chatId, UUID currentUserId) {
        if (chatId == null) {
            return;
        }
        if (chatId <= 0) {
            throw new BadRequestException("Telegram Chat ID phải là số nguyên dương");
        }
        boolean used = currentUserId == null
                ? userRepository.existsByTelegramChatId(chatId)
                : userRepository.existsByTelegramChatIdAndIdNot(chatId, currentUserId);
        if (used) {
            throw new BadRequestException("Telegram Chat ID đã được liên kết với tài khoản khác");
        }
    }

    private String normalizeZaloChatId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return null;
        }
        return chatId.trim();
    }

    private void validateZaloChatId(String chatId, UUID currentUserId) {
        String normalized = normalizeZaloChatId(chatId);
        if (normalized == null) {
            return;
        }
        if (normalized.length() > 100 || !normalized.matches("[A-Za-z0-9._-]{1,100}")) {
            throw new BadRequestException("Zalo Chat ID không hợp lệ");
        }
        boolean used = currentUserId == null
                ? userRepository.existsByZaloChatId(normalized)
                : userRepository.existsByZaloChatIdAndIdNot(normalized, currentUserId);
        if (used) {
            throw new BadRequestException("Zalo Chat ID đã được liên kết với tài khoản khác");
        }
    }

    private void publishUserNotification(User user, UUID actorId, String type, String subject, String message) {
        List<NotificationEvent> events = new java.util.ArrayList<>();
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            NotificationEvent event = new NotificationEvent(type, user.getEmail().trim(), user.getUsername(),
                    subject, message, user.getId());
            event.setActorId(actorId);
            event.setRecipientId(user.getId());
            events.add(event);
        }
        if (user.getTelegramChatId() != null) {
            NotificationEvent event = new NotificationEvent(type, null, user.getUsername(), subject, message,
                    user.getId(), user.getTelegramChatId());
            event.setActorId(actorId);
            event.setRecipientId(user.getId());
            events.add(event);
        }
        if (user.getZaloChatId() != null && !user.getZaloChatId().isBlank()) {
            NotificationEvent event = new NotificationEvent(type, null, user.getUsername(), subject, message,
                    user.getId());
            event.setZaloChatId(user.getZaloChatId());
            event.setActorId(actorId);
            event.setRecipientId(user.getId());
            events.add(event);
        }
        if (events.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.forEach(notificationProducer::send);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(notificationProducer::send);
            }
        });
    }

    private void publishTelegramLinked(User user, UUID actorId) {
        String email = maskEmail(user.getEmail());
        publishTelegramNotification(user, actorId, "TELEGRAM_ACCOUNT_LINKED",
                "Liên kết Telegram thành công",
                "Tài khoản của bạn vừa được liên kết với Telegram.\n"
                        + "Tên đăng nhập: " + user.getUsername() + "\n"
                        + "Email: " + (email == null ? "Chưa có email" : email));
    }

    private void publishZaloLinked(User user, UUID actorId) {
        String email = maskEmail(user.getEmail());
        publishZaloNotification(user, actorId, "ZALO_ACCOUNT_LINKED",
                "Liên kết Zalo thành công",
                "Tài khoản của bạn vừa được liên kết với Zalo.\n"
                        + "Tên đăng nhập: " + user.getUsername() + "\n"
                        + "Email: " + (email == null ? "Chưa có email" : email));
    }

    private void publishRoleChanges(User user, UUID actorId, Set<String> oldRoles, Set<String> newRoles) {
        Set<String> added = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        added.addAll(newRoles);
        added.removeAll(oldRoles);
        Set<String> removed = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        removed.addAll(oldRoles);
        removed.removeAll(newRoles);

        StringBuilder message = new StringBuilder("Quyền của tài khoản ")
                .append(user.getUsername()).append(" vừa được cập nhật.");
        if (!added.isEmpty()) {
            message.append("\nQuyền được thêm: ").append(String.join(", ", added));
        }
        if (!removed.isEmpty()) {
            message.append("\nQuyền bị gỡ: ").append(String.join(", ", removed));
        }
        if (user.getTelegramChatId() != null) {
            publishTelegramNotification(user, actorId, "USER_ROLES_CHANGED",
                    "Quyền tài khoản đã thay đổi", message.toString());
        }
        if (user.getZaloChatId() != null) {
            publishZaloNotification(user, actorId, "USER_ROLES_CHANGED",
                    "Quyền tài khoản đã thay đổi", message.toString());
        }
    }

    private void publishTelegramNotification(User user, UUID actorId, String type, String subject, String message) {
        NotificationEvent event = new NotificationEvent(type, null, user.getUsername(), subject,
                message, user.getId(), user.getTelegramChatId());
        event.setActorId(actorId);
        event.setRecipientId(user.getId());
        sendAfterCommit(event);
    }

    private void publishZaloNotification(User user, UUID actorId, String type, String subject, String message) {
        NotificationEvent event = new NotificationEvent(type, null, user.getUsername(), subject,
                message, user.getId());
        event.setZaloChatId(user.getZaloChatId());
        event.setActorId(actorId);
        event.setRecipientId(user.getId());
        sendAfterCommit(event);
    }

    private void sendAfterCommit(NotificationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationProducer.send(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationProducer.send(event);
            }
        });
    }

    @Transactional
    public java.util.Map<String, Object> generateBotLinkCode(UUID userId, String channel) {
        String cleanChannel = "ZALO".equalsIgnoreCase(channel) ? "ZALO" : "TELEGRAM";
        int randomCode = 100000 + new java.security.SecureRandom().nextInt(900000);
        String code = String.valueOf(randomCode);

        if (jdbcTemplate != null) {
            jdbcTemplate.update("DELETE FROM user_bot_link_codes WHERE user_id = ? AND channel = ?", userId, cleanChannel);
            java.time.Instant expiresAt = java.time.Instant.now().plus(java.time.Duration.ofMinutes(10));
            jdbcTemplate.update("""
                    INSERT INTO user_bot_link_codes (code, user_id, channel, expires_at)
                    VALUES (?, ?, ?, ?)
                    """, code, userId, cleanChannel, java.sql.Timestamp.from(expiresAt));

            String botUsername = System.getenv("TELEGRAM_BOT_USERNAME");
            if (botUsername == null || botUsername.isBlank()) {
                botUsername = "khaosat1120_bot";
            }
            String deepLink = null;
            String appLink = null;
            if ("TELEGRAM".equals(cleanChannel)) {
                deepLink = "https://web.telegram.org/k/#@" + botUsername.replace("@", "");
                appLink = "https://t.me/" + botUsername.replace("@", "") + "?start=link_" + code;
            } else if ("ZALO".equals(cleanChannel)) {
                deepLink = "https://zalo.me/1585703035743826375";
            }

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("code", code);
            result.put("channel", cleanChannel);
            result.put("expiresAt", expiresAt);
            result.put("expiresInSeconds", 600);
            if (deepLink != null) {
                result.put("deepLink", deepLink);
            }
            if (appLink != null) {
                result.put("appLink", appLink);
            }
            return result;
        }

        return java.util.Map.of("code", code, "channel", cleanChannel);
    }

    private Set<String> roleNames(User user) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        user.getRoles().stream().map(Role::getName).filter(java.util.Objects::nonNull)
                .forEach(names::add);
        return names;
    }

    // =========================
    // PROFILE MANAGEMENT
    // =========================

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại hoặc đã bị khóa"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getBotNotificationEnabled() != null) {
            user.setBotNotificationEnabled(request.getBotNotificationEnabled());
        }

        if (request.getEmail() != null) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
                    throw new BadRequestException("Email đã được sử dụng bởi tài khoản khác");
                }
                user.setEmail(normalizedEmail);
            } else if (normalizedEmail == null) {
                user.setEmail(null);
            }
        }

        User updated = userRepository.save(user);
        return toUserResponse(updated);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại hoặc đã bị khóa"));

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new BadRequestException("Vui lòng nhập mật khẩu hiện tại");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Xác nhận mật khẩu mới không khớp");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse unlinkBot(UUID userId, String channel) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại hoặc đã bị khóa"));

        String cleanChannel = channel != null ? channel.trim().toUpperCase() : "";
        if ("TELEGRAM".equals(cleanChannel)) {
            user.setTelegramChatId(null);
        } else if ("ZALO".equals(cleanChannel)) {
            user.setZaloChatId(null);
        } else {
            throw new BadRequestException("Kênh không hợp lệ (hỗ trợ TELEGRAM hoặc ZALO)");
        }

        if (jdbcTemplate != null) {
            jdbcTemplate.update("DELETE FROM user_bot_link_codes WHERE user_id = ? AND channel = ?", userId, cleanChannel);
        }

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse setBotNotifications(UUID userId, boolean enabled) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại hoặc đã bị khóa"));

        user.setBotNotificationEnabled(enabled);
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }
}
