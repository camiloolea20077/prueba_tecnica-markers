package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.util.Set;

/**
 * Datos públicos del usuario (nunca incluye la contraseña).
 */
public record UserResponse(Long id, String fullName, String email, String role, String roleName,
                           Set<String> permissions) {
}
