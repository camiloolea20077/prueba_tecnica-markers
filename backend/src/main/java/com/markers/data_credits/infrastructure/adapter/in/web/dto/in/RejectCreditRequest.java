package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rechazo: motivo obligatorio que verá el solicitante.
 */
public record RejectCreditRequest(
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
        String reason) {
}
