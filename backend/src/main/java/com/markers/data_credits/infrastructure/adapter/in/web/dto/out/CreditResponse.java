package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Crédito con su estado y condiciones.
 *
 * @param estimated        {@code true} si tasa/cuota son estimadas con la tasa sugerida (aún no aprobado)
 * @param annualRate       tasa EA aplicada (aprobado) o sugerida (resto)
 */
public record CreditResponse(Long id,
                             ApplicantResponse applicant,
                             BigDecimal amount,
                             int termMonths,
                             String status,
                             String statusLabel,
                             boolean estimated,
                             BigDecimal suggestedAnnualRate,
                             BigDecimal annualRate,
                             BigDecimal monthlyRate,
                             BigDecimal monthlyPayment,
                             BigDecimal totalInterest,
                             BigDecimal totalPayable,
                             String rejectionReason,
                             Instant createdAt,
                             Instant decidedAt) {

    public record ApplicantResponse(Long id, String fullName, String email) {
    }
}
