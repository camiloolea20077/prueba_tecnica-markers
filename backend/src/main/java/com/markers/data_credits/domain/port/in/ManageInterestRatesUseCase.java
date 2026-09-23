package com.markers.data_credits.domain.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.markers.data_credits.domain.model.InterestRateTier;

/**
 * Caso de uso del administrador: CRUD de tramos de tasa EA.
 */
public interface ManageInterestRatesUseCase {

    /** Todos los tramos (activos e inactivos), por plazo mínimo. */
    List<InterestRateTier> findAll();

    /**
     * @throws com.markers.data_credits.domain.exception.CreditRuleException si los datos son inválidos o el rango
     *         se superpone con otro tramo activo
     */
    InterestRateTier create(TierCommand command);

    /**
     * @throws com.markers.data_credits.domain.exception.InterestRateTierNotFoundException si no existe
     */
    InterestRateTier update(Long id, TierCommand command);

    void delete(Long id);

    record TierCommand(String name, Integer minTermMonths, Integer maxTermMonths, BigDecimal annualEffectiveRate,
                       Boolean active) {

        public boolean isActive() {
            return active == null || active;
        }
    }
}
