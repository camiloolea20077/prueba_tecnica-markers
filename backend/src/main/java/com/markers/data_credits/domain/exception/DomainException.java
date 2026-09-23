package com.markers.data_credits.domain.exception;

/**
 * Excepción base de las reglas de negocio. La capa web la traduce a un código HTTP.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
