package com.markers.data_credits.domain.port.out;

import java.util.Optional;

import com.markers.data_credits.domain.model.PageResult;
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

    Optional<User> findById(Long id);

    /** Búsqueda paginada por texto (nombre/correo), rol y estado; filtros {@code null} no restringen. */
    PageResult<User> search(String query, String roleCode, Boolean active, int page, int size);

    /** true si otro usuario (distinto de {@code excludeId}) ya usa el correo. */
    boolean existsByEmail(String email, Long excludeId);

    long countActiveByRole(String roleCode);

    boolean hasCredits(Long userId);

    /** Inserta o actualiza; el rol se resuelve por su código. */
    User save(User user);

    void deleteById(Long id);
}
