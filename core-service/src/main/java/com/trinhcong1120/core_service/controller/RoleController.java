package com.trinhcong1120.core_service.controller;

import com.trinhcong1120.core_service.dto.role.CreateRoleRequest;
import com.trinhcong1120.core_service.dto.role.RoleDetailResponse;
import com.trinhcong1120.core_service.dto.role.RoleResponse;
import com.trinhcong1120.core_service.dto.role.UpdateRoleRequest;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/Roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(
            RoleService roleService) {

        this.roleService = roleService;
    }
    @PreAuthorize("hasAuthority('role_view')")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(
                roleService.getAllRoles()
        );
    }

    @PreAuthorize("hasAuthority('role_view')")
    @GetMapping("/{id}")
    public ResponseEntity<RoleDetailResponse> getRoleById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                roleService.getRoleById(id)
        );
    }

    @PreAuthorize("hasAuthority('role_create')")
    @PostMapping
    public ResponseEntity<Role> createRole(
            @Valid @RequestBody CreateRoleRequest request) {

        return ResponseEntity.ok(
                roleService.createRole(request)
        );
    }

    @PreAuthorize("hasAuthority('role_update')")
    @PutMapping("/{id}")
    public ResponseEntity<String> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {

        roleService.updateRole(id, request);

        return ResponseEntity.ok(
                "Cập nhật role "
                        + id
                        + " thành công"
        );
    }

    @PreAuthorize("hasAuthority('role_delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRole(
            @PathVariable Integer id) {

        roleService.deleteRole(id);

        return ResponseEntity.ok(
                "Đã xóa role " + id
        );
    }
}
