package com.trinhcong1120.core_service.service;

import com.trinhcong1120.core_service.dto.menu.CreateMenuRequest;
import com.trinhcong1120.core_service.dto.menu.MenuResponse;
import com.trinhcong1120.core_service.dto.menu.MenuTreeResponse;
import com.trinhcong1120.core_service.dto.menu.UpdateMenuRequest;
import com.trinhcong1120.core_service.entity.Menu;
import com.trinhcong1120.core_service.entity.Permission;
import com.trinhcong1120.core_service.entity.Role;
import com.trinhcong1120.core_service.entity.User;
import com.trinhcong1120.core_service.exception.BadRequestException;
import com.trinhcong1120.core_service.exception.NotFoundException;
import com.trinhcong1120.core_service.repository.FunctionRepository;
import com.trinhcong1120.core_service.repository.MenuRepository;
import com.trinhcong1120.core_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final FunctionRepository functionRepository;
    private final UserRepository userRepository;

    public MenuService(
            MenuRepository menuRepository,
            FunctionRepository functionRepository,
            UserRepository userRepository) {

        this.menuRepository = menuRepository;
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {

        return menuRepository
                .findAllByOrderByOrderIndexAsc()
                .stream()
                .map(this::toMenuResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuTreeResponse> getMenuTree() {

        List<Menu> menus =
                menuRepository
                        .findAllByOrderByOrderIndexAsc();

        return buildTree(menus);
    }

    @Transactional(readOnly = true)
    public List<MenuTreeResponse> getMyMenu(
            Integer userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "User không tồn tại"
                        )
                );

        Set<Integer> functionIds =
                user.getRoles()
                        .stream()
                        .flatMap(
                                role ->
                                        role.getPermissions()
                                                .stream()
                        )
                        .map(Permission::getFunction)
                        .filter(Objects::nonNull)
                        .map(
                                com.trinhcong1120
                                        .core_service
                                        .entity
                                        .Function::getId
                        )
                        .collect(Collectors.toSet());

        List<Menu> allowedMenus =
                menuRepository
                        .findAllByOrderByOrderIndexAsc()
                        .stream()
                        .filter(menu ->
                                menu.getFunction() == null
                                        ||
                                        functionIds.contains(
                                                menu.getFunction()
                                                        .getId()
                                        )
                        )
                        .toList();

        return buildTree(allowedMenus);
    }

    @Transactional(readOnly = true)
    public Menu getMenuById(Integer id) {

        return menuRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Menu không tồn tại"
                        )
                );
    }

    @Transactional
    public Integer createMenu(
            CreateMenuRequest request) {

        if (request.getName() == null
                || request.getName().isEmpty()) {

            throw new BadRequestException(
                    "Tên menu không được để trống"
            );
        }

        Menu menu = new Menu();

        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setIcon(request.getIcon());
        menu.setOrderIndex(
                request.getOrderIndex()
        );
        menu.setCreatedAt(
                LocalDateTime.now()
        );

        if (request.getParentId() != null) {

            Menu parent = menuRepository
                    .findById(request.getParentId())
                    .orElse(null);

            menu.setParent(parent);
        }

        if (request.getFunctionId() != null) {

            com.trinhcong1120.core_service.entity.Function function =
                    functionRepository
                            .findById(
                                    request.getFunctionId()
                            )
                            .orElse(null);

            menu.setFunction(function);
        }

        Menu saved =
                menuRepository.save(menu);

        return saved.getId();
    }

    @Transactional
    public void updateMenu(
            Integer id,
            UpdateMenuRequest request) {

        Menu menu = menuRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Menu không tồn tại"
                        )
                );

        if (request.getName() != null
                && !request.getName().isBlank()) {

            menu.setName(
                    request.getName()
            );
        }

        if (request.getPath() != null) {
            menu.setPath(
                    request.getPath()
            );
        }

        if (request.getIcon() != null) {
            menu.setIcon(
                    request.getIcon()
            );
        }

        if (request.getOrderIndex() != null) {
            menu.setOrderIndex(
                    request.getOrderIndex()
            );
        }

        if (request.getParentId() != null) {

            Menu parent =
                    menuRepository
                            .findById(
                                    request.getParentId()
                            )
                            .orElse(null);

            menu.setParent(parent);
        }

        if (request.getFunctionId() != null) {

            com.trinhcong1120.core_service.entity.Function function =
                    functionRepository
                            .findById(
                                    request.getFunctionId()
                            )
                            .orElse(null);

            menu.setFunction(function);
        }

        menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(Integer id) {

        Menu menu = menuRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Menu không tồn tại"
                        )
                );

        if (menuRepository
                .existsByParent_Id(id)) {

            throw new BadRequestException(
                    "Menu đang có menu con, không thể xóa"
            );
        }

        menuRepository.delete(menu);
    }

    private MenuResponse toMenuResponse(
            Menu menu) {

        Integer parentId =
                menu.getParent() == null
                        ? null
                        : menu.getParent().getId();

        Integer functionId =
                menu.getFunction() == null
                        ? null
                        : menu.getFunction().getId();

        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPath(),
                parentId,
                menu.getIcon(),
                menu.getOrderIndex(),
                menu.getCreatedAt(),
                functionId
        );
    }

    private List<MenuTreeResponse> buildTree(
            List<Menu> menus) {

        return menus.stream()
                .filter(menu ->
                        menu.getParent() == null
                                ||
                                menus.stream()
                                        .noneMatch(
                                                other ->
                                                        other.getId()
                                                                .equals(
                                                                        menu.getParent()
                                                                                .getId()
                                                                )
                                        )
                )
                .sorted(
                        Comparator.comparing(
                                Menu::getOrderIndex,
                                Comparator.nullsLast(
                                        Integer::compareTo
                                )
                        )
                )
                .map(menu ->
                        buildNode(
                                menu,
                                menus
                        )
                )
                .toList();
    }

    private MenuTreeResponse buildNode(
            Menu menu,
            List<Menu> menus) {

        Integer functionId =
                menu.getFunction() == null
                        ? null
                        : menu.getFunction().getId();

        MenuTreeResponse node =
                new MenuTreeResponse(
                        menu.getId(),
                        menu.getName(),
                        menu.getPath(),
                        menu.getIcon(),
                        menu.getOrderIndex(),
                        functionId
                );

        List<MenuTreeResponse> children =
                menus.stream()
                        .filter(child ->
                                child.getParent() != null
                                        &&
                                        child.getParent()
                                                .getId()
                                                .equals(
                                                        menu.getId()
                                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        Menu::getOrderIndex,
                                        Comparator.nullsLast(
                                                Integer::compareTo
                                        )
                                )
                        )
                        .map(child ->
                                buildNode(
                                        child,
                                        menus
                                )
                        )
                        .toList();

        node.setChildren(children);

        return node;
    }
}