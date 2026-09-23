package com.markers.data_credits.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.service.InterestCalculator;

/**
 * Crédito solicitado por un usuario. Inmutable: cada transición devuelve una nueva instancia.
 *
 * @param suggestedAnnualRate tasa EA del tramo al momento de solicitar (%)
 * @param annualEffectiveRate tasa EA aplicada al aprobar (%), {@code null} si no está aprobado
 * @param monthlyRate         tasa mensual equivalente (%), definida al aprobar
 * @param monthlyPayment      cuota definitiva, definida al aprobar
 * @param version             versión para bloqueo optimista
 */
public record Credit(Long id,
                     CreditApplicant applicant,
                     BigDecimal amount,
                     int termMonths,
                     BigDecimal suggestedAnnualRate,
                     BigDecimal annualEffectiveRate,
                     BigDecimal monthlyRate,
                     BigDecimal monthlyPayment,
                     BigDecimal totalInterest,
                     BigDecimal totalPayable,
                     CreditStatus status,
                     String rejectionReason,
                     Long decidedBy,
                     Instant decidedAt,
                     Instant createdAt,
                     Long version) {

    public static final int MIN_REASON_LENGTH = 10;
    public static final int MAX_REASON_LENGTH = 500;

    /**
     * Crea una solicitud nueva en estado {@link CreditStatus#PENDING}.
     *
     * @throws CreditRuleException si el monto o el plazo están fuera de la política
     */
    public static Credit request(CreditApplicant applicant, BigDecimal amount, Integer termMonths,
                                 InterestRateTier tier, CreditPolicy policy) {
        Objects.requireNonNull(applicant, "applicant");
        validateAmountAndTerm(amount, termMonths, policy);
        if (tier == null || !tier.covers(termMonths)) {
            throw new CreditRuleException("No hay una tasa configurada para un plazo de " + termMonths + " meses");
        }
        return new Credit(null, applicant, amount.setScale(2, RoundingMode.HALF_UP), termMonths,
                tier.annualEffectiveRate(), null, null, null, null, null,
                CreditStatus.PENDING, null, null, null, null, null);
    }

    /** Valida monto y plazo contra la política (se usa también en la simulación). */
    public static void validateAmountAndTerm(BigDecimal amount, Integer termMonths, CreditPolicy policy) {
        if (!policy.isAmountAllowed(amount)) {
            throw new CreditRuleException(String.format(
                    "El monto debe estar entre %,.0f y %,.0f", policy.minAmount(), policy.maxAmount()));
        }
        if (!policy.isTermAllowed(termMonths)) {
            throw new CreditRuleException(String.format(
                    "El plazo debe estar entre %d y %d meses", policy.minTermMonths(), policy.maxTermMonths()));
        }
    }

    /**
     * El dueño cancela su solicitud mientras está pendiente.
     *
     * @throws CreditNotFoundException     si el crédito no es del usuario (no se revela que existe)
     * @throws InvalidCreditStateException si ya no está pendiente
     */
    public Credit cancel(Long requesterId) {
        if (!isOwnedBy(requesterId)) {
            throw new CreditNotFoundException(id);
        }
        requirePending("cancelar");
        return withStatus(CreditStatus.CANCELLED);
    }

    /**
     * Aprueba la solicitud con la tasa efectiva anual que define el analista y fija las condiciones
     * definitivas (tasa mensual, cuota, intereses, total).
     *
     * @throws InvalidCreditStateException si ya no está pendiente
     * @throws CreditRuleException         si la tasa está fuera de la política o el analista es el solicitante
     */
    public Credit approve(Long adminId, BigDecimal annualRate, CreditPolicy policy) {
        requirePending("aprobar");
        requireNotSelfDecision(adminId);
        if (!policy.isRateAllowed(annualRate)) {
            throw new CreditRuleException(policy.rateRangeMessage());
        }
        CreditQuote q = InterestCalculator.quote(amount, termMonths, annualRate, false);
        return new Credit(id, applicant, amount, termMonths, suggestedAnnualRate, q.annualRate(), q.monthlyRate(),
                q.monthlyPayment(), q.totalInterest(), q.totalPayable(), CreditStatus.APPROVED, null, adminId,
                Instant.now(), createdAt, version);
    }

    /**
     * Rechaza la solicitud; el motivo es obligatorio (10 a 500 caracteres).
     *
     * @throws InvalidCreditStateException si ya no está pendiente
     * @throws CreditRuleException         si el motivo no es válido o el analista es el solicitante
     */
    public Credit reject(Long adminId, String reason) {
        requirePending("rechazar");
        requireNotSelfDecision(adminId);
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() < MIN_REASON_LENGTH || cleanReason.length() > MAX_REASON_LENGTH) {
            throw new CreditRuleException("El motivo del rechazo debe tener entre " + MIN_REASON_LENGTH + " y "
                    + MAX_REASON_LENGTH + " caracteres");
        }
        return new Credit(id, applicant, amount, termMonths, suggestedAnnualRate, null, null, null, null, null,
                CreditStatus.REJECTED, cleanReason, adminId, Instant.now(), createdAt, version);
    }

    public boolean isOwnedBy(Long userId) {
        return applicant != null && Objects.equals(applicant.id(), userId);
    }

    public boolean isPending() {
        return status == CreditStatus.PENDING;
    }

    /**
     * Condiciones del crédito: definitivas si fue aprobado; si no, estimadas con la tasa sugerida.
     */
    public CreditQuote quote() {
        if (status == CreditStatus.APPROVED && annualEffectiveRate != null) {
            return new CreditQuote(amount, termMonths, annualEffectiveRate, monthlyRate, monthlyPayment,
                    totalInterest, totalPayable, null);
        }
        return InterestCalculator.quote(amount, termMonths, suggestedAnnualRate, false);
    }

    /** true si las condiciones de {@link #quote()} son una estimación (aún no aprobado). */
    public boolean isEstimated() {
        return status != CreditStatus.APPROVED;
    }

    /** Control de cuatro ojos: nadie decide sobre su propia solicitud. */
    private void requireNotSelfDecision(Long adminId) {
        if (isOwnedBy(adminId)) {
            throw new CreditRuleException("No puedes decidir sobre tu propia solicitud de crédito");
        }
    }

    private void requirePending(String action) {
        if (!isPending()) {
            throw new InvalidCreditStateException(status, action);
        }
    }

    private Credit withStatus(CreditStatus newStatus) {
        return new Credit(id, applicant, amount, termMonths, suggestedAnnualRate, annualEffectiveRate, monthlyRate,
                monthlyPayment, totalInterest, totalPayable, newStatus, rejectionReason, decidedBy, decidedAt,
                createdAt, version);
    }
}
