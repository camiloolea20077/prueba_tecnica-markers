package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.CreditEntity;

public interface CreditJpaRepository extends JpaRepository<CreditEntity, Long> {

    /** Carga el crédito junto con su solicitante. */
    @EntityGraph(attributePaths = "user")
    Optional<CreditEntity> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    List<CreditEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<CreditEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CreditStatus status);

    long countByUserIdAndStatus(Long userId, CreditStatus status);
}
