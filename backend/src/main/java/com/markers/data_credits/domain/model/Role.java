package com.markers.data_credits.domain.model;

import java.util.Set;

/**
 * Rol del sistema con el conjunto de permisos que otorga.
 *
 * @param id          identificador del rol
 * @param code        código único del rol (ver {@link RoleCodes})
 * @param name        nombre legible
 * @param permissions códigos de permiso otorgados (ver {@link Permissions})
 */
public record Role(Long id, String code, String name, Set<String> permissions) {

    public Role {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAdmin() {
        return RoleCodes.ADMIN.equals(code);
    }
}
