package com.markers.data_credits.domain.port.in;

import java.math.BigDecimal;

import com.markers.data_credits.domain.model.Credit;

/**
 * Caso de uso: el usuario solicita un crédito indicando monto y plazo.
 */
public interface RequestCreditUseCase {

    /**
     * @return crédito creado en estado PENDING con la tasa sugerida del tramo
     * @throws com.markers.data_credits.domain.exception.CreditRuleException si incumple monto, plazo,
     *         tramo o límite de solicitudes pendientes
     */
    Credit request(RequestCreditCommand command);

    record RequestCreditCommand(Long userId, BigDecimal amount, Integer termMonths) {
    }
}
