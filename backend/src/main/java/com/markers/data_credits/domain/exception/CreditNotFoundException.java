package com.markers.data_credits.domain.exception;

/**
 * El crédito no existe o no pertenece al usuario que lo consulta (no se revela cuál). HTTP 404.
 */
public class CreditNotFoundException extends DomainException {

    public CreditNotFoundException(Long id) {
        super("No existe el crédito " + id);
    }
}
