package com.markers.data_credits.domain.model;

/**
 * Resultado de un inicio de sesión: token emitido y usuario autenticado.
 */
public record AuthSession(AuthToken token, User user) {
}
