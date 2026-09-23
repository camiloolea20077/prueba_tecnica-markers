package com.markers.data_credits.infrastructure.security;

import java.util.Set;

/**
 * Principal que queda en el {@code SecurityContext} tras validar el JWT.
 * Se inyecta en los controladores con {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(Long id, String email, String fullName, String role, Set<String> permissions) {
}
