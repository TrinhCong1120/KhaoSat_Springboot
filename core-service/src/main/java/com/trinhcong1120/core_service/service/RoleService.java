package com.trinhcong1120.core_service.service;

import com.trinhcong1120.core_service.dto.role.CreateRoleRequest;
import com.trinhcong1120.core_service.dto.role.RoleDetailResponse;
import com.trinhcong1120.core_service.dto.role.RoleResponse;
import com.trinhcong1120.core_service.dto.role.UpdateRoleRequest;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.exception.NotFoundException;
import com.trinhcong1120.core_service.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(
            RoleRepository roleRepository) {

        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {

        return roleRepository.findAll()
                .stream()
                .map(role ->
                        new RoleResponse(
                                role.getId(),
                                role.getName(),
                                role.getPermissions()
                                        .stream()
                                        .map(permission ->
                                                permission.getCode()
                                        )
                                        .distinct()
                                        .toList()
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getRoleById(
            Integer id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Role không tồn tại"
                        )
                );

        List<RoleDetailResponse.PermissionInfo>
                permissions =
                role.getPermissions()
                        .stream()
                        .map(permission ->
                                new RoleDetailResponse.PermissionInfo(
                                        permission.getId(),
                                        permission.getCode(),
                                        permission.getName()
                                )
                        )
                        .toList();

        return new RoleDetailResponse(
                role.getId(),
                role.getName(),
                permissions
        );
    }

    @Transactional
    public Role createRole(
            CreateRoleRequest request) {

        if (roleRepository.existsByName(
                request.getName())) {

            throw new BadRequestException(
                    "Tên role đã tồn tại"
            );
        }

        Role role = new Role();
        role.setName(request.getName());

        return roleRepository.save(role);
    }

    @Transactional
    public void updateRole(
            Integer id,
            UpdateRoleRequest request) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Role không tồn tại"
                        )
                );

        if (roleRepository.existsByNameAndIdNot(
                request.getName(),
                id)) {

            throw new BadRequestException(
                    "Tên role đã tồn tại"
            );
        }

        role.setName(request.getName());

        roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Integer id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Role không tồn tại"
                        )
                );

        roleRepository.delete(role);
    }
}