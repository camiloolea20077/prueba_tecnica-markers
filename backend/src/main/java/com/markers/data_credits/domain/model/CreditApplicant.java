package com.markers.data_credits.domain.model;

/**
 * Datos básicos del solicitante de un crédito.
 */
public record CreditApplicant(Long id, String fullName, String email) {
}
