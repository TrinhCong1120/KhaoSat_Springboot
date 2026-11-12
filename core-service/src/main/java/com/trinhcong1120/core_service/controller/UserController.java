package com.trinhcong1120.core_service.controller;

import java.util.UUID;

import com.trinhcong1120.core_service.dto.user.CreateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateProfileRequest;
import com.trinhcong1120.core_service.dto.user.ChangePasswordRequest;
import com.trinhcong1120.core_service.dto.user.UserSearchResponse;
import com.trinhcong1120.core_service.dto.user.UserResponse;
import com.trinhcong1120.core_service.service.UserService;
import com.trinhcong1120.core_service.dto.common.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('user_view') or hasAuthority('ROLE_admin') or hasRole('admin')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @PreAuthorize("hasAuthority('user_view') or hasAuthority('ROLE_admin') or hasRole('admin')")
    @GetMapping("/page")
    public ResponseEntity<PagedResponse<UserResponse>> getUsersPage(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(userService.getUsersPage(page));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            Authentication authentication
    ) {
        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return ResponseEntity.ok(
                userService.getActiveUser(userId)
        );
    }

    @PostMapping("/me/bot-link-code")
    public ResponseEntity<Map<String, Object>> generateBotLinkCode(
            Authentication authentication,
            @RequestParam(defaultValue = "TELEGRAM") String channel
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userService.generateBotLinkCode(userId, channel));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @DeleteMapping("/me/bot-link")
    public ResponseEntity<UserResponse> unlinkBot(
            @RequestParam String channel,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userService.unlinkBot(userId, channel));
    }

    @PutMapping("/me/bot-notifications")
    public ResponseEntity<UserResponse> setBotNotifications(
            @RequestParam boolean enabled,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userService.setBotNotifications(userId, enabled));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(userService.searchActiveUsers(q));
    }

    @PreAuthorize("hasAuthority('user_create') or hasAuthority('ROLE_admin') or hasRole('admin')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {

        UUID userId =
                userService.createUser(request, UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(
                Map.of(
                        "message", "Tạo user thành công",
                        "userId", userId
                )
        );
    }

    @PreAuthorize("hasAuthority('user_update') or hasAuthority('ROLE_admin') or hasRole('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        userService.updateUser(id, request, UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(
                "Cập nhật user "
                        + id
                        + " thành công"
        );
    }

    @PreAuthorize("hasAuthority('user_delete') or hasAuthority('ROLE_admin') or hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable UUID id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(
                "Đã xóa user " + id
        );
    }
}
