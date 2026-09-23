package com.markers.data_credits.infrastructure.exception;

import org.springframework.http.HttpStatus;

/**
 * Error técnico o de infraestructura con un código HTTP explícito.
 * Para reglas de negocio usar las excepciones de {@code domain.exception}.
 */
public class GlobalException extends RuntimeException {

    private final HttpStatus status;

    public GlobalException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
