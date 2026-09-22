package com.trinhcong1120.core_service.controller;

import com.trinhcong1120.core_service.dto.menu.CreateMenuRequest;
import com.trinhcong1120.core_service.dto.menu.MenuResponse;
import com.trinhcong1120.core_service.dto.menu.MenuTreeResponse;
import com.trinhcong1120.core_service.dto.menu.UpdateMenuRequest;
import com.trinhcong1120.core_service.entity.Menu;
import com.trinhcong1120.core_service.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(
            MenuService menuService) {

        this.menuService = menuService;
    }

    @PreAuthorize("hasAuthority('menu_view')")
    @GetMapping
    public ResponseEntity<List<MenuResponse>>
    getAllMenus() {

        return ResponseEntity.ok(
                menuService.getAllMenus()
        );
    }

    @PreAuthorize("hasAuthority('menu_view')")
    @GetMapping("/tree")
    public ResponseEntity<List<MenuTreeResponse>>
    getMenuTree() {

        return ResponseEntity.ok(
                menuService.getMenuTree()
        );
    }

    @GetMapping("/my-menu")
    public ResponseEntity<List<MenuTreeResponse>>
    getMyMenu(Authentication authentication) {

        Integer userId =
                Integer.valueOf(
                        authentication.getName()
                );

        return ResponseEntity.ok(
                menuService.getMyMenu(userId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> getMenuById(@PathVariable Integer id) {

        Menu menu = menuService.getMenuById(id);

        MenuResponse response = new MenuResponse();

        response.setId(menu.getId());
        response.setName(menu.getName());
        response.setPath(menu.getPath());
        response.setParentId(
                menu.getParent() != null
                        ? menu.getParent().getId()
                        : null
        );
        response.setIcon(menu.getIcon());
        response.setOrderIndex(menu.getOrderIndex());
        response.setCreatedAt(menu.getCreatedAt());
        response.setFunctionId(
                menu.getFunction() != null
                        ? menu.getFunction().getId()
                        : null
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('menu_create')")
    @PostMapping
    public ResponseEntity<Map<String, Object>>
    createMenu(
            @Valid @RequestBody CreateMenuRequest request) {

        Integer menuId =
                menuService.createMenu(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Tạo menu thành công",
                        "menuId",
                        menuId
                )
        );
    }

    @PreAuthorize("hasAuthority('menu_update')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>>
    updateMenu(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateMenuRequest request) {

        menuService.updateMenu(
                id,
                request
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Cập nhật menu thành công",
                        "menuId",
                        id
                )
        );
    }

    @PreAuthorize("hasAuthority('menu_delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String>
    deleteMenu(
            @PathVariable Integer id) {

        menuService.deleteMenu(id);

        return ResponseEntity.ok(
                "Đã xóa menu " + id
        );
    }
}
