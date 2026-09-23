package com.markers.data_credits.support;

import java.time.Instant;
import java.util.Set;

import com.markers.data_credits.domain.model.Permissions;
import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.model.RoleCodes;
import com.markers.data_credits.domain.model.User;

/**
 * Usuarios de prueba reutilizables.
 */
public final class TestUsers {

    public static final String PASSWORD_HASH = "$2a$10$hash";
    public static final Instant CREATED_AT = Instant.parse("2026-09-01T12:00:00Z");

    public static final Role USER_ROLE = new Role(1L, RoleCodes.USER, "Usuario",
            Set.of(Permissions.CREDIT_REQUEST, Permissions.CREDIT_VIEW_OWN,
                    Permissions.CREDIT_CANCEL_OWN, Permissions.CREDIT_SIMULATE));

    public static final Role ADMIN_ROLE = new Role(2L, RoleCodes.ADMIN, "Administrador",
            Set.of(Permissions.CREDIT_SIMULATE, Permissions.CREDIT_VIEW_ALL, Permissions.CREDIT_APPROVE,
                    Permissions.CREDIT_REJECT, Permissions.USER_MANAGE, Permissions.RATE_MANAGE));

    private TestUsers() {
    }

    public static User user() {
        return new User(1L, "Usuario Demo", "usuario@test.com", PASSWORD_HASH, true, USER_ROLE, CREATED_AT);
    }

    public static User admin() {
        return new User(2L, "Administrador", "admin@test.com", PASSWORD_HASH, true, ADMIN_ROLE, CREATED_AT);
    }

    public static User inactiveUser() {
        return new User(3L, "Inactivo", "inactivo@test.com", PASSWORD_HASH, false, USER_ROLE, CREATED_AT);
    }
}
