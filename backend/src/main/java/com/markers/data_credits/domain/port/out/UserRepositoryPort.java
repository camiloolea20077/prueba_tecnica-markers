package com.markers.data_credits.domain.port.out;

import java.util.Optional;

import com.markers.data_credits.domain.model.User;

/**
 * Puerto de salida para la persistencia de usuarios.
 */
public interface UserRepositoryPort {

    /**
     * Busca un usuario por correo (sin distinguir mayúsculas) con su rol y permisos.
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario bloqueando su fila hasta el fin de la transacción ({@code SELECT … FOR UPDATE}).
     * Serializa operaciones concurrentes del mismo usuario (p. ej. el límite de solicitudes pendientes).
     */
    Optional<User> findByIdForUpdate(Long id);
}
