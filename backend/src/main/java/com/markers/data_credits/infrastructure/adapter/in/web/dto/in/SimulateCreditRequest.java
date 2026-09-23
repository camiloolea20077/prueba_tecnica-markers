package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Simulación de cuota.
 *
 * @param annualRate      tasa EA (%) opcional; si no llega se usa la del tramo del plazo
 * @param includeSchedule incluir tabla de amortización (por defecto {@code true})
 */
public record SimulateCreditRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 2 decimales")
        BigDecimal amount,

        @NotNull(message = "El plazo es obligatorio")
        @Positive(message = "El plazo debe ser mayor a cero")
        Integer termMonths,

        @Positive(message = "La tasa debe ser mayor a cero")
        @Digits(integer = 3, fraction = 4, message = "La tasa admite máximo 4 decimales")
        BigDecimal annualRate,

        Boolean includeSchedule) {

    public boolean scheduleRequested() {
        return includeSchedule == null || includeSchedule;
    }
}
