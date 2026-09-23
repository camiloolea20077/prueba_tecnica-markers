package com.markers.data_credits.domain.model;

import java.time.Instant;
import java.util.Locale;

/**
 * Usuario del sistema.
 *
 * @param id           identificador
 * @param fullName     nombre completo
 * @param email        correo (usado como nombre de usuario, siempre en minúsculas)
 * @param passwordHash contraseña cifrada con BCrypt
 * @param active       indica si puede iniciar sesión
 * @param role         rol asignado con sus permisos
 * @param createdAt    fecha de creación ({@code null} si aún no se guarda)
 */
public record User(Long id, String fullName, String email, String passwordHash, boolean active, Role role,
                   Instant createdAt) {

    /** Usuario nuevo con datos normalizados (nombre sin espacios sobrantes, correo en minúsculas). */
    public static User create(String fullName, String email, String passwordHash, Role role, boolean active) {
        return new User(null, normalizeName(fullName), normalizeEmail(email), passwordHash, active, role, null);
    }

    /** Copia con datos editados; conserva id, fecha de creación y, si no llega una nueva, la contraseña. */
    public User withChanges(String newFullName, String newEmail, Role newRole, boolean newActive,
                            String newPasswordHash) {
        return new User(id, normalizeName(newFullName), normalizeEmail(newEmail),
                newPasswordHash != null ? newPasswordHash : passwordHash, newActive, newRole, createdAt);
    }

    public User withActive(boolean newActive) {
        return new User(id, fullName, email, passwordHash, newActive, role, createdAt);
    }

    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }
}
