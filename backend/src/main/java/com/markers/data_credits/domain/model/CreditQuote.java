package com.markers.data_credits.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de calcular un crédito con tasa efectiva anual y cuota fija.
 *
 * @param annualRate     tasa efectiva anual (%)
 * @param monthlyRate    tasa mensual vencida equivalente (%, 6 decimales)
 * @param monthlyPayment cuota fija mensual
 * @param totalInterest  intereses totales
 * @param totalPayable   total a pagar (capital + intereses)
 * @param schedule       tabla de amortización (vacía si no se solicitó)
 */
public record CreditQuote(BigDecimal amount,
                          int termMonths,
                          BigDecimal annualRate,
                          BigDecimal monthlyRate,
                          BigDecimal monthlyPayment,
                          BigDecimal totalInterest,
                          BigDecimal totalPayable,
                          List<AmortizationRow> schedule) {

    public CreditQuote {
        schedule = schedule == null ? List.of() : List.copyOf(schedule);
    }

    public CreditQuote withoutSchedule() {
        return new CreditQuote(amount, termMonths, annualRate, monthlyRate, monthlyPayment, totalInterest,
                totalPayable, List.of());
    }
}
