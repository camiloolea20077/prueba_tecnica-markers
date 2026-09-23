package com.markers.data_credits.domain.port.in;

import com.markers.data_credits.domain.model.Credit;

/**
 * Caso de uso: el dueño cancela una solicitud pendiente.
 */
public interface CancelCreditUseCase {

    /**
     * @throws com.markers.data_credits.domain.exception.CreditNotFoundException     si no existe o no es suyo
     * @throws com.markers.data_credits.domain.exception.InvalidCreditStateException si ya no está pendiente
     */
    Credit cancel(Long creditId, Long userId);
}
