package com.markers.data_credits.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;

/**
 * Puerto de salida para la persistencia de créditos.
 */
public interface CreditRepositoryPort {

    Credit save(Credit credit);

    Optional<Credit> findById(Long id);

    /** {@code status} opcional; orden: más reciente primero. */
    List<Credit> findByUserId(Long userId, CreditStatus status);

    long countByUserIdAndStatus(Long userId, CreditStatus status);
}
