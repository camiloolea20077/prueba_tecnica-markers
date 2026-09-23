package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

/**
 * Filtros opcionales para la búsqueda de usuarios. Un filtro {@code null} no restringe.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> matches(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\'));
        };
    }

    public static Specification<UserEntity> hasRole(String roleCode) {
        return (root, query, cb) -> roleCode == null ? null : cb.equal(root.get("role").get("code"), roleCode);
    }

    public static Specification<UserEntity> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
