package com.markers.data_credits.domain.port.in;

import java.math.BigDecimal;

import com.markers.data_credits.domain.model.Credit;

/**
 * Caso de uso del analista: aprobar o rechazar solicitudes pendientes (transaccional).
 */
public interface DecideCreditUseCase {

    /**
     * Aprueba con la tasa EA indicada y fija cuota y totales.
     *
     * @throws com.markers.data_credits.domain.exception.CreditNotFoundException     si no existe
     * @throws com.markers.data_credits.domain.exception.InvalidCreditStateException si ya fue decidido
     * @throws com.markers.data_credits.domain.exception.CreditRuleException         si la tasa está fuera de rango
     */
    Credit approve(ApproveCreditCommand command);

    /**
     * Rechaza con un motivo obligatorio.
     *
     * @throws com.markers.data_credits.domain.exception.CreditNotFoundException     si no existe
     * @throws com.markers.data_credits.domain.exception.InvalidCreditStateException si ya fue decidido
     * @throws com.markers.data_credits.domain.exception.CreditRuleException         si el motivo no es válido
     */
    Credit reject(RejectCreditCommand command);

    record ApproveCreditCommand(Long creditId, Long adminId, BigDecimal annualRate) {
    }

    record RejectCreditCommand(Long creditId, Long adminId, String reason) {
    }
}
