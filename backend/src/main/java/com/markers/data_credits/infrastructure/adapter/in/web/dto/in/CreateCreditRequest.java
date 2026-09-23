package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud de crédito. Aquí se valida el formato (400);
 * los rangos de negocio (monto/plazo configurables) los valida el dominio (422).
 */
public record CreateCreditRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 2 decimales")
        BigDecimal amount,

        @NotNull(message = "El plazo es obligatorio")
        @Positive(message = "El plazo debe ser mayor a cero")
        Integer termMonths) {
}
