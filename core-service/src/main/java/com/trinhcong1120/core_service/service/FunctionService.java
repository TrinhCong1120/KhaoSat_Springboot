package com.trinhcong1120.core_service.service;

import java.util.UUID;

import com.trinhcong1120.core_service.dto.function.FunctionDetailResponse;
import com.trinhcong1120.core_service.dto.function.FunctionResponse;
import com.trinhcong1120.core_service.dto.function.PermissionStatusResponse;
import com.trinhcong1120.core_service.dto.function.UpdatePermissionRequest;
import com.trinhcong1120.core_service.entity.Permission;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.exception.NotFoundException;
import com.trinhcong1120.core_service.repository.FunctionRepository;
import com.trinhcong1120.core_service.repository.PermissionRepository;
import com.trinhcong1120.core_service.repository.RoleRepository;
import com.trinhcong1120.core_service.dto.common.PagedResponse;
import com.trinhcong1120.core_service.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FunctionService {

    private static final int PAGE_SIZE = 10;

    private final FunctionRepository functionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public FunctionService(
            FunctionRepository functionRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {

        this.functionRepository = functionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<FunctionResponse> getAllFunctions() {

        return functionRepository.findAll()
                .stream()
                .map(function ->
                        new FunctionResponse(
                                function.getId(),
                                function.getName(),
                                function.getCode()
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<FunctionResponse> getFunctionsPage(int page) {
        if (page < 1) throw new BadRequestException("page phai bat dau tu 1");
        return PagedResponse.from(functionRepository
                .findAll(PageRequest.of(page - 1, PAGE_SIZE,
                        Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))))
                .map(function -> new FunctionResponse(function.getId(), function.getName(), function.getCode())));
    }

    @Transactional(readOnly = true)
    public List<FunctionDetailResponse> getFunctionDetail(
            UUID functionId) {

        com.trinhcong1120.core_service.entity.Function function =
                functionRepository.findById(functionId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Function không tồn tại"
                                )
                        );

        List<Permission> functionPermissions =
                function.getPermissions();

        return roleRepository.findAll()
                .stream()
                .map(role -> {

                    List<PermissionStatusResponse> permissions =
                            functionPermissions.stream()
                                    .map(permission ->
                                            new PermissionStatusResponse(
                                                    permission.getId(),
                                                    permission.getCode(),
                                                    permission.getType(),
                                                    role.getPermissions()
                                                            .contains(permission)
                                            )
                                    )
                                    .toList();

                    return new FunctionDetailResponse(
                            function.getId(),
                            function.getName(),
                            role.getId(),
                            role.getName(),
                            permissions
                    );
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> updatePermission(
            UpdatePermissionRequest request) {

        Role role = roleRepository
                .findById(request.getRoleId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Role không tồn tại"
                        )
                );

        Permission permission =
                permissionRepository
                        .findById(request.getPermissionId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Permission không tồn tại"
                                )
                        );

        if (Boolean.TRUE.equals(
                request.getIsActive())) {

            role.getPermissions().add(permission);

        } else {

            role.getPermissions().remove(permission);
        }

        roleRepository.save(role);

        return Map.of(
                "message", "Cập nhật thành công",
                "roleId", role.getId(),
                "permissionId", permission.getId(),
                "isActive",
                Boolean.TRUE.equals(
                        request.getIsActive()
                )
        );
    }
}
