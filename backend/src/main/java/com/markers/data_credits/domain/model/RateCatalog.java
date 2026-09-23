package com.markers.data_credits.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Condiciones vigentes para solicitar un crédito: tramos de tasa y límites de la política.
 * El front las usa para validar formularios sin duplicar la configuración.
 */
public record RateCatalog(List<InterestRateTier> tiers,
                          BigDecimal minAnnualRate,
                          BigDecimal maxAnnualRate,
                          BigDecimal minAmount,
                          BigDecimal maxAmount,
                          int minTermMonths,
                          int maxTermMonths) {

    public RateCatalog {
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }

    public static RateCatalog of(List<InterestRateTier> tiers, CreditPolicy policy) {
        return new RateCatalog(tiers, policy.minAnnualRate(), policy.maxAnnualRate(), policy.minAmount(),
                policy.maxAmount(), policy.minTermMonths(), policy.maxTermMonths());
    }
}
