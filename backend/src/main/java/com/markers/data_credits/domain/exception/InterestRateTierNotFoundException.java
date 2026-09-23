package com.markers.data_credits.domain.exception;

/**
 * No existe el tramo de tasa indicado. HTTP 404.
 */
public class InterestRateTierNotFoundException extends DomainException {

    public InterestRateTierNotFoundException(Long id) {
        super("No existe el tramo de tasa " + id);
    }
}
