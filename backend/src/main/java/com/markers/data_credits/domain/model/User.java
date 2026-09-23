package com.markers.data_credits.domain.model;

/**
 * Usuario del sistema.
 *
 * @param id           identificador
 * @param fullName     nombre completo
 * @param email        correo (usado como nombre de usuario)
 * @param passwordHash contraseña cifrada con BCrypt
 * @param active       indica si puede iniciar sesión
 * @param role         rol asignado con sus permisos
 */
public record User(Long id, String fullName, String email, String passwordHash, boolean active, Role role) {
}
