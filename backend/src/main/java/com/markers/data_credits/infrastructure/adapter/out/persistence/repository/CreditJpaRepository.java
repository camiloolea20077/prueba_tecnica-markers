package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.CreditEntity;

public interface CreditJpaRepository extends JpaRepository<CreditEntity, Long>, JpaSpecificationExecutor<CreditEntity> {

    /** Carga el crédito junto con su solicitante. */
    @EntityGraph(attributePaths = "user")
    Optional<CreditEntity> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    List<CreditEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<CreditEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CreditStatus status);

    long countByUserIdAndStatus(Long userId, CreditStatus status);

    /** Búsqueda dinámica (filtros opcionales) cargando el solicitante para evitar N+1. */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<CreditEntity> findAll(Specification<CreditEntity> spec, Pageable pageable);

    /** Filas {@code [CreditStatus, Long]}. */
    @Query("select c.status, count(c) from CreditEntity c group by c.status")
    List<Object[]> countGroupedByStatus();
}
