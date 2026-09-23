package com.markers.data_credits.domain.port.in;

import java.math.BigDecimal;

import com.markers.data_credits.domain.model.CreditQuote;

/**
 * Caso de uso: simular la cuota de un crédito sin guardar nada.
 */
public interface SimulateCreditUseCase {

    /**
     * Si {@code annualRate} es {@code null} se usa la tasa del tramo que corresponde al plazo.
     *
     * @throws com.markers.data_credits.domain.exception.CreditRuleException si monto, plazo o tasa están fuera de rango
     */
    CreditQuote simulate(SimulateCreditCommand command);

    record SimulateCreditCommand(BigDecimal amount, Integer termMonths, BigDecimal annualRate,
                                 boolean includeSchedule) {
    }
}
