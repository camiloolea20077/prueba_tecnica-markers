package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /** Carga usuario, rol y permisos en una sola consulta. */
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /**
     * {@code SELECT … FOR UPDATE} sobre el usuario (sin joins: PostgreSQL no permite FOR UPDATE
     * en el lado nullable de un outer join). Rol y permisos se cargan de forma perezosa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
}
