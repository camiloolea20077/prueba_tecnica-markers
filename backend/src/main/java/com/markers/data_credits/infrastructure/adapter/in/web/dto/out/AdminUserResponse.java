package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.time.Instant;

/**
 * Usuario visto por el administrador (nunca incluye la contraseña).
 */
public record AdminUserResponse(Long id, String fullName, String email, String role, String roleName, boolean active,
                                Instant createdAt) {
}
