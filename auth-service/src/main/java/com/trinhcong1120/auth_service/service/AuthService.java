package com.trinhcong1120.auth_service.service;

import com.trinhcong1120.auth_service.dto.LoginRequest;
import com.trinhcong1120.auth_service.dto.LoginResponse;
import com.trinhcong1120.auth_service.entity.Permission;
import com.trinhcong1120.auth_service.entity.Role;
import com.trinhcong1120.auth_service.entity.User;
import com.trinhcong1120.auth_service.exception.BadRequestException;
import com.trinhcong1120.auth_service.exception.UnauthorizedException;
import com.trinhcong1120.auth_service.repository.UserRepository;
import com.trinhcong1120.auth_service.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 1. Kiểm tra username/password
        if (request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new BadRequestException(
                    "Thiếu username hoặc password"
            );
        }

        // 2. Tìm user đang hoạt động
        // 3. Không tồn tại -> 401
        User user = userRepository
                .findByUsernameAndIsActiveTrue(
                        request.getUsername()
                )
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User không tồn tại"
                        )
                );

        // 4 + 5. Verify BCrypt
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new UnauthorizedException(
                    "Sai mật khẩu"
            );
        }

        // 6. Lấy role
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .distinct()
                .toList();

        // 7. Lấy permission từ tất cả role
        // và loại bỏ permission trùng
        List<String> permissions = user.getRoles()
                .stream()
                .flatMap(role ->
                        role.getPermissions().stream()
                )
                .filter(permission ->
                        Boolean.TRUE.equals(
                                permission.getIsActive()
                        )
                )
                .map(Permission::getCode)
                .distinct()
                .toList();

        // 8. Tạo JWT
        // false = 2 giờ
        // true  = 30 ngày
        String token = jwtService.generateToken(
                user,
                roles,
                permissions,
                request.isRememberMe()
        );

        // 9. Response
        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                roles,
                permissions
        );
    }
}