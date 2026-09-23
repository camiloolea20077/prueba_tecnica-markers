package com.markers.data_credits.domain.model;

import java.math.BigDecimal;

/**
 * Tramo de tasa: la tasa efectiva anual sugerida para un rango de plazos.
 *
 * @param annualEffectiveRate tasa EA en porcentaje (ej. 22.0000 = 22 %)
 */
public record InterestRateTier(Long id,
                               String name,
                               int minTermMonths,
                               int maxTermMonths,
                               BigDecimal annualEffectiveRate,
                               boolean active) {

    public boolean covers(int termMonths) {
        return termMonths >= minTermMonths && termMonths <= maxTermMonths;
    }
}
