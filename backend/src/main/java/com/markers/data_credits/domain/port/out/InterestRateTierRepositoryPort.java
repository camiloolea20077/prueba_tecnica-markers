package com.markers.data_credits.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.markers.data_credits.domain.model.InterestRateTier;

/**
 * Puerto de salida para los tramos de tasa.
 */
public interface InterestRateTierRepositoryPort {

    /** Tramos activos ordenados por plazo mínimo. */
    List<InterestRateTier> findActive();

    /** Tramo activo cuyo rango de plazo contiene {@code termMonths}. */
    Optional<InterestRateTier> findActiveForTerm(int termMonths);
}
