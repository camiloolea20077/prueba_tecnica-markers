package com.markers.data_credits.domain.exception;

/**
 * No existe un usuario con el identificador indicado.
 */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String identifier) {
        super("No existe el usuario " + identifier);
    }
}
