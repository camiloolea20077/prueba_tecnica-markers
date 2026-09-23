package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

/**
 * Cantidad de créditos por estado (tablero del analista).
 */
public record CreditSummaryResponse(long pending, long approved, long rejected, long cancelled, long total) {
}
