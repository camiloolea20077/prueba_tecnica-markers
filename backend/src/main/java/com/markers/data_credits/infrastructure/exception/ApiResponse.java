package com.markers.data_credits.infrastructure.exception;

import org.springframework.http.HttpStatus;

/**
 * Envoltorio estándar de todas las respuestas de la API.
 *
 * @param status  código HTTP
 * @param message mensaje descriptivo
 * @param error   {@code true} si la respuesta representa un error
 * @param data    carga útil (puede ser {@code null})
 * @param <T>     tipo de la carga útil
 */
public record ApiResponse<T>(int status, String message, boolean error, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, false, data);
    }

    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, false, data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, true, data);
    }

    public static ApiResponse<Object> error(HttpStatus status, String message) {
        return error(status, message, null);
    }
}
