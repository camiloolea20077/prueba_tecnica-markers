package com.markers.data_credits.infrastructure.adapter.out.persistence.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.CreditEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

import jakarta.persistence.criteria.Join;

/**
 * Filtros opcionales para la búsqueda de créditos del administrador.
 * Un filtro {@code null} no restringe (evita parámetros nulos en SQL).
 */
public final class CreditSpecifications {

    private CreditSpecifications() {
    }

    public static Specification<CreditEntity> hasStatus(CreditStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** Coincidencia parcial, sin distinguir mayúsculas, en nombre o correo del solicitante. */
    public static Specification<CreditEntity> applicantMatches(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            Join<CreditEntity, UserEntity> user = root.join("user");
            return cb.or(
                    cb.like(cb.lower(user.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(user.get("email")), pattern, '\\'));
        };
    }
}
