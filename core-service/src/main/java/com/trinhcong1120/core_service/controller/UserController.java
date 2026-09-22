package com.trinhcong1120.core_service.controller;

import com.trinhcong1120.core_service.dto.user.CreateUserRequest;
import com.trinhcong1120.core_service.dto.user.UpdateUserRequest;
import com.trinhcong1120.core_service.dto.user.UserResponse;
import com.trinhcong1120.core_service.service.UserService;
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

    @PreAuthorize("hasAuthority('user_view')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            Authentication authentication
    ) {
        Integer userId =
                Integer.valueOf(
                        authentication.getName()
                );

        return ResponseEntity.ok(
                userService.getActiveUser(userId)
        );
    }

    @PreAuthorize("hasAuthority('user_create')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        Integer userId =
                userService.createUser(request);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Tạo user thành công",
                        "userId", userId
                )
        );
    }

    @PreAuthorize("hasAuthority('user_update')")
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request) {

        userService.updateUser(id, request);

        return ResponseEntity.ok(
                "Cập nhật user "
                        + id
                        + " thành công"
        );
    }

    @PreAuthorize("hasAuthority('user_delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Integer id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(
                "Đã xóa user " + id
        );
    }
}
