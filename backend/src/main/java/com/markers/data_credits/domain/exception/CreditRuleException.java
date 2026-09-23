package com.markers.data_credits.domain.exception;

/**
 * Regla de negocio de créditos incumplida (monto, plazo, tasa, límite de pendientes…). HTTP 422.
 */
public class CreditRuleException extends DomainException {

    public CreditRuleException(String message) {
        super(message);
    }
}
