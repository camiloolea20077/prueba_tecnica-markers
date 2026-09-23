package com.markers.data_credits.infrastructure.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.DomainException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.exception.InactiveUserException;
import com.markers.data_credits.domain.exception.InvalidCredentialsException;
import com.markers.data_credits.domain.exception.UserNotFoundException;

/**
 * Traduce las excepciones a respuestas HTTP con formato {@link ApiResponse}.
 */
@RestControllerAdvice
public class GlobalHandlerException {

    private static final Logger log = LoggerFactory.getLogger(GlobalHandlerException.class);

    /** Errores de Bean Validation: devuelve el mensaje de cada campo inválido. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(WebExchangeBindException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getGlobalErrors().forEach(e -> errors.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", errors);
    }

    /** Cuerpo ausente, JSON mal formado o parámetros con tipo incorrecto. */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiResponse<Object>> handleInput(ServerWebInputException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo o los parámetros de la solicitud no son válidos", null);
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ApiResponse<Object>> handleInactive(InactiveUserException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    /** Lanzada por {@code @PreAuthorize} cuando falta el rol o el permiso. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción", null);
    }

    @ExceptionHandler({UserNotFoundException.class, CreditNotFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNotFound(DomainException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidCreditStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(InvalidCreditStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Dos usuarios modificaron el mismo registro a la vez (@Version). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT,
                "El registro fue modificado por otro usuario. Actualice e intente de nuevo", null);
    }

    /** Cualquier otra regla de negocio incumplida. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomain(DomainException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobal(GlobalException ex) {
        return build(ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return build(status, message, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado, intente más tarde", null);
    }

    private static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponse.error(status, message, data));
    }
}
