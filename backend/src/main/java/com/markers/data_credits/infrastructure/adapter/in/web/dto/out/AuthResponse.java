package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.time.Instant;

/**
 * Respuesta del login: token Bearer, su expiración y el usuario autenticado.
 */
public record AuthResponse(String token, String tokenType, Instant expiresAt, UserResponse user) {
}
