package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.util.Set;

/**
 * Rol con sus permisos (para el formulario de usuarios).
 */
public record RoleResponse(String code, String name, Set<String> permissions) {
}
