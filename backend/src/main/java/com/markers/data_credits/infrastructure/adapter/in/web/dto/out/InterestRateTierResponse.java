package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.math.BigDecimal;

/**
 * Tramo de tasa (vista del administrador, incluye si está activo).
 */
public record InterestRateTierResponse(Long id, String name, int minTermMonths, int maxTermMonths,
                                       BigDecimal annualEffectiveRate, boolean active) {
}
