package com.markers.data_credits.domain.exception;

import com.markers.data_credits.domain.model.CreditStatus;

/**
 * La acción no es válida para el estado actual del crédito. HTTP 409.
 */
public class InvalidCreditStateException extends DomainException {

    public InvalidCreditStateException(CreditStatus current, String action) {
        super("No se puede " + action + " un crédito en estado " + current.label().toLowerCase());
    }
}
