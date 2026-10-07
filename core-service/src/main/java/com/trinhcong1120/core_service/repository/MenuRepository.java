package com.trinhcong1120.core_service.repository;

import java.util.UUID;

import com.trinhcong1120.core_service.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository
        extends JpaRepository<Menu, UUID> {

    List<Menu> findAllByOrderByOrderIndexAsc();

    List<Menu> findByParent_IdOrderByOrderIndexAsc(
            UUID parentId
    );

    boolean existsByParent_Id(
            UUID parentId
    );
}