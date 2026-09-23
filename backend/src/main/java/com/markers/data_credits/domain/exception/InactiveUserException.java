package com.markers.data_credits.domain.exception;

/**
 * El usuario existe pero está deshabilitado.
 */
public class InactiveUserException extends DomainException {

    public InactiveUserException() {
        super("El usuario se encuentra inactivo");
    }
}
