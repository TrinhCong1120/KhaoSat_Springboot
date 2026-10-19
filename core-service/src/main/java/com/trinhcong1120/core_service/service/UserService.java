package com.trinhcong1120.core_service.service;

import java.util.UUID;

import com.trinhcong1120.core_service.dto.event.NotificationEvent;
import com.trinhcong1120.core_service.dto.user.CreateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
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

        validateTelegramChatId(request.getTelegramChatId(), null);
        user.setTelegramChatId(request.getTelegramChatId());

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

        if (request.isTelegramChatIdPresent()) {
            validateTelegramChatId(request.getTelegramChatId(), id);
            user.setTelegramChatId(request.getTelegramChatId());
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

        Set<String> newRoles = roleNames(savedUser);
        if (!oldRoles.equals(newRoles) && savedUser.getTelegramChatId() != null) {
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
                user.getTelegramChatId()
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
        publishTelegramNotification(user, actorId, "USER_ROLES_CHANGED",
                "Quyền tài khoản đã thay đổi", message.toString());
    }

    private void publishTelegramNotification(User user, UUID actorId, String type, String subject, String message) {
        NotificationEvent event = new NotificationEvent(type, null, user.getUsername(), subject,
                message, user.getId(), user.getTelegramChatId());
        event.setActorId(actorId);
        event.setRecipientId(user.getId());
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

    private Set<String> roleNames(User user) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        user.getRoles().stream().map(Role::getName).filter(java.util.Objects::nonNull)
                .forEach(names::add);
        return names;
    }
}
