package com.markers.data_credits.domain.model;

import java.time.Instant;

/**
 * Token de acceso emitido tras una autenticación exitosa.
 *
 * @param value     token firmado
 * @param expiresAt instante de expiración
 */
public record AuthToken(String value, Instant expiresAt) {
}
