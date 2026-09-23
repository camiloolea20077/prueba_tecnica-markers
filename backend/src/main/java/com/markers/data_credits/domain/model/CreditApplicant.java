package com.markers.data_credits.domain.model;

import java.io.Serializable;

/**
 * Datos básicos del solicitante de un crédito.
 */
public record CreditApplicant(Long id, String fullName, String email) implements Serializable {
}
