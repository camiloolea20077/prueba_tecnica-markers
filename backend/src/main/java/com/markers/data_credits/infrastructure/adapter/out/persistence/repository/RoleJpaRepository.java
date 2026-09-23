package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.RoleEntity;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findByCode(String code);
}
