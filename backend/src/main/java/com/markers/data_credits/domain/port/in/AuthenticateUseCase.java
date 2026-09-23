package com.markers.data_credits.domain.port.in;

import com.markers.data_credits.domain.model.AuthSession;

/**
 * Caso de uso: iniciar sesión con correo y contraseña.
 */
public interface AuthenticateUseCase {

    /**
     * Valida las credenciales y emite un token de acceso.
     *
     * @param command correo y contraseña
     * @return sesión con el token y el usuario autenticado
     * @throws com.markers.data_credits.domain.exception.InvalidCredentialsException si las credenciales no coinciden
     * @throws com.markers.data_credits.domain.exception.InactiveUserException       si el usuario está inactivo
     */
    AuthSession login(LoginCommand command);

    record LoginCommand(String email, String password) {
    }
}
