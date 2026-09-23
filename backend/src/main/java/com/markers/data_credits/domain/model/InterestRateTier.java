package com.markers.data_credits.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.markers.data_credits.domain.exception.CreditRuleException;

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
                               boolean active) implements Serializable {

    /**
     * Crea un tramo validando nombre, rango de plazo y tasa contra la política.
     *
     * @throws CreditRuleException si algún dato está fuera de la política
     */
    public static InterestRateTier create(Long id, String name, Integer minTermMonths, Integer maxTermMonths,
                                          BigDecimal annualEffectiveRate, boolean active, CreditPolicy policy) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 80) {
            throw new CreditRuleException("El nombre del tramo es obligatorio (máximo 80 caracteres)");
        }
        if (minTermMonths == null || maxTermMonths == null || minTermMonths > maxTermMonths) {
            throw new CreditRuleException("El plazo mínimo no puede ser mayor que el plazo máximo");
        }
        if (minTermMonths < policy.minTermMonths() || maxTermMonths > policy.maxTermMonths()) {
            throw new CreditRuleException(String.format("El rango del tramo debe estar entre %d y %d meses",
                    policy.minTermMonths(), policy.maxTermMonths()));
        }
        if (!policy.isRateAllowed(annualEffectiveRate)) {
            throw new CreditRuleException(policy.rateRangeMessage());
        }
        return new InterestRateTier(id, cleanName, minTermMonths, maxTermMonths,
                annualEffectiveRate.setScale(4, RoundingMode.HALF_UP), active);
    }

    public boolean covers(int termMonths) {
        return termMonths >= minTermMonths && termMonths <= maxTermMonths;
    }

    /** true si ambos rangos de plazo comparten al menos un mes. */
    public boolean overlaps(InterestRateTier other) {
        return minTermMonths <= other.maxTermMonths && maxTermMonths >= other.minTermMonths;
    }
}
