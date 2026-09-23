package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import jakarta.validation.constraints.NotNull;

/**
 * Activar o desactivar un usuario.
 */
public record ChangeUserStatusRequest(@NotNull(message = "Indica si el usuario queda activo") Boolean active) {
}
