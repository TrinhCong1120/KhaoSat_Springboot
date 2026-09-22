package com.trinhcong1120.core_service.repository;

import com.trinhcong1120.core_service.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository
        extends JpaRepository<Menu, Integer> {

    List<Menu> findAllByOrderByOrderIndexAsc();

    List<Menu> findByParent_IdOrderByOrderIndexAsc(
            Integer parentId
    );

    boolean existsByParent_Id(
            Integer parentId
    );
}