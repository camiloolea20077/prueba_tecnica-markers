package com.markers.data_credits.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditApplicant;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.CreditEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.InterestRateTierEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

/**
 * Conversión entidad JPA → modelo de dominio de créditos y tramos de tasa.
 * (dominio → entidad se hace en el adaptador porque depende del estado de la sesión JPA)
 */
@Mapper
public interface CreditPersistenceMapper {

    CreditApplicant toApplicant(UserEntity user);

    InterestRateTier toDomain(InterestRateTierEntity entity);

    /**
     * Manual: MapStruct confunde las transiciones del agregado (p. ej. {@code cancel(Long)})
     * con setters fluidos.
     */
    default Credit toDomain(CreditEntity e) {
        if (e == null) {
            return null;
        }
        return new Credit(
                e.getId(),
                toApplicant(e.getUser()),
                e.getAmount(),
                e.getTermMonths(),
                e.getSuggestedAnnualRate(),
                e.getAnnualEffectiveRate(),
                e.getMonthlyRate(),
                e.getMonthlyPayment(),
                e.getTotalInterest(),
                e.getTotalPayable(),
                e.getStatus(),
                e.getRejectionReason(),
                e.getDecidedBy() == null ? null : e.getDecidedBy().getId(),
                e.getDecidedAt(),
                e.getCreatedAt(),
                e.getVersion());
    }
}
