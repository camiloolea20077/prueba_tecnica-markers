package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Aprobación: tasa efectiva anual (%) que asigna el analista. El rango permitido lo valida el dominio (422).
 */
public record ApproveCreditRequest(
        @NotNull(message = "La tasa efectiva anual es obligatoria")
        @Positive(message = "La tasa debe ser mayor a cero")
        @Digits(integer = 3, fraction = 4, message = "La tasa admite máximo 4 decimales")
        BigDecimal annualRate) {
}
