package com.markers.data_credits.domain.exception;

/**
 * Credenciales incorrectas. El mensaje es genérico para no revelar si el correo existe.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
