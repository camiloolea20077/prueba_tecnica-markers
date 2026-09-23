package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tramos de tasa vigentes y límites de tasa EA, monto y plazo.
 */
public record RateCatalogResponse(BigDecimal minAnnualRate, BigDecimal maxAnnualRate,
                                  BigDecimal minAmount, BigDecimal maxAmount,
                                  int minTermMonths, int maxTermMonths,
                                  List<TierResponse> tiers) {

    public record TierResponse(Long id, String name, int minTermMonths, int maxTermMonths,
                               BigDecimal annualEffectiveRate) {
    }
}
