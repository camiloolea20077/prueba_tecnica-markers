package com.markers.data_credits.domain.exception;

/**
 * Ya existe un usuario con ese correo. HTTP 409.
 */
public class EmailAlreadyExistsException extends DomainException {

    public EmailAlreadyExistsException(String email) {
        super("Ya existe un usuario con el correo " + email);
    }
}
