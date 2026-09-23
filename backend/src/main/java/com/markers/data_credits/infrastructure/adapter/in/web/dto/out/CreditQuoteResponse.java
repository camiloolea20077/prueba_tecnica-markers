package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de la simulación.
 */
public record CreditQuoteResponse(BigDecimal amount,
                                  int termMonths,
                                  BigDecimal annualRate,
                                  BigDecimal monthlyRate,
                                  BigDecimal monthlyPayment,
                                  BigDecimal totalInterest,
                                  BigDecimal totalPayable,
                                  List<AmortizationRowResponse> schedule) {

    public record AmortizationRowResponse(int period, BigDecimal payment, BigDecimal interest,
                                          BigDecimal principal, BigDecimal balance) {
    }
}
