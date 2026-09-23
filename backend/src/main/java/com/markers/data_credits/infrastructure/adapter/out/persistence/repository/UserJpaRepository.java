package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

import jakarta.persistence.LockModeType;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    /** Carga usuario, rol y permisos en una sola consulta. */
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<UserEntity> findWithRoleById(Long id);

    /**
     * {@code SELECT … FOR UPDATE} sobre el usuario (sin joins: PostgreSQL no permite FOR UPDATE
     * en el lado nullable de un outer join). Rol y permisos se cargan de forma perezosa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    /** Solo el rol en el grafo: incluir la colección de permisos rompería la paginación en SQL. */
    @Override
    @EntityGraph(attributePaths = "role")
    Page<UserEntity> findAll(Specification<UserEntity> spec, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByRoleCodeAndActiveTrue(String roleCode);
}
