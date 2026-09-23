package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un tramo de tasa. Rangos y superposición los valida el dominio (422).
 */
public record InterestRateTierRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre admite máximo 80 caracteres")
        String name,

        @NotNull(message = "El plazo mínimo es obligatorio")
        @Positive(message = "El plazo mínimo debe ser mayor a cero")
        Integer minTermMonths,

        @NotNull(message = "El plazo máximo es obligatorio")
        @Positive(message = "El plazo máximo debe ser mayor a cero")
        Integer maxTermMonths,

        @NotNull(message = "La tasa efectiva anual es obligatoria")
        @Positive(message = "La tasa debe ser mayor a cero")
        @Digits(integer = 3, fraction = 4, message = "La tasa admite máximo 4 decimales")
        BigDecimal annualEffectiveRate,

        Boolean active) {
}
