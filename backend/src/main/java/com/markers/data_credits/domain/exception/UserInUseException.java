package com.markers.data_credits.domain.exception;

/**
 * El usuario tiene créditos asociados y no puede eliminarse (se sugiere desactivarlo). HTTP 409.
 */
public class UserInUseException extends DomainException {

    public UserInUseException() {
        super("El usuario tiene créditos registrados y no puede eliminarse; desactívalo en su lugar");
    }
}
