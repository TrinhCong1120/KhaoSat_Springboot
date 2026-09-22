package com.trinhcong1120.core_service.service;

import com.trinhcong1120.core_service.dto.event.NotificationEvent;
import com.trinhcong1120.core_service.dto.user.CreateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
import com.trinhcong1120.core_service.dto.user.UserResponse;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.producer.NotificationProducer;
import com.trinhcong1120.core_service.repository.RoleRepository;
import com.trinhcong1120.core_service.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer notificationProducer;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            NotificationProducer notificationProducer
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationProducer = notificationProducer;
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
    public UserResponse getActiveUser(Integer id) {

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

    // =========================
    // CREATE USER
    // =========================

    @Transactional
    public Integer createUser(CreateUserRequest request) {

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

        // =========================
        // GỬI KAFKA EVENT
        // =========================

        if (savedUser.getEmail() != null
                && !savedUser.getEmail().isBlank()) {

            NotificationEvent event =
                    new NotificationEvent(
                            "USER_CREATED",
                            savedUser.getEmail(),
                            savedUser.getUsername(),
                            "Tài khoản đã được tạo",
                            "Xin chào "
                                    + savedUser.getUsername()
                                    + ", tài khoản của bạn đã được tạo thành công.",
                            savedUser.getId()
                    );

            notificationProducer.send(event);
        }

        return savedUser.getId();
    }

    // =========================
    // UPDATE USER
    // =========================

    @Transactional
    public void updateUser(
            Integer id,
            UpdateUserRequest request
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

        // =========================
        // SAVE
        // =========================

        User savedUser =
                userRepository.save(user);

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
        if (wasActive
                && isNowInactive
                && savedUser.getEmail() != null
                && !savedUser.getEmail().isBlank()) {

            NotificationEvent event =
                    new NotificationEvent(
                            "USER_DEACTIVATED",
                            savedUser.getEmail(),
                            savedUser.getUsername(),
                            "Tài khoản đã bị khóa",
                            "Xin chào "
                                    + savedUser.getUsername()
                                    + ", tài khoản của bạn đã bị khóa.",
                            savedUser.getId()
                    );

            notificationProducer.send(event);
        }
    }

    // =========================
    // DELETE USER
    // =========================

    @Transactional
    public void deleteUser(Integer id) {

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
                roles
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
}
