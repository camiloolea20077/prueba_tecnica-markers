package com.markers.data_credits.domain.port.in;

import com.markers.data_credits.domain.model.User;

/**
 * Caso de uso: obtener el usuario autenticado con su rol y permisos vigentes.
 */
public interface GetCurrentUserUseCase {

    /**
     * @param email correo del usuario autenticado
     * @return usuario con rol y permisos
     * @throws com.markers.data_credits.domain.exception.UserNotFoundException si ya no existe
     */
    User getCurrentUser(String email);
}
