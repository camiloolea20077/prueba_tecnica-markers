package com.markers.data_credits.domain.port.in;

import java.util.List;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;

/**
 * Caso de uso: consultar créditos y su estado.
 */
public interface QueryCreditUseCase {

    /** Créditos del usuario, del más reciente al más antiguo; {@code status} opcional. */
    List<Credit> findByUser(Long userId, CreditStatus status);

    /**
     * Detalle de un crédito. Solo el dueño o quien puede ver todos los créditos.
     *
     * @throws com.markers.data_credits.domain.exception.CreditNotFoundException si no existe o no tiene acceso
     */
    Credit findById(Long creditId, Requester requester);

    /** Quién consulta: su id y si puede ver créditos de otros usuarios. */
    record Requester(Long userId, boolean canViewAll) {
    }
}
