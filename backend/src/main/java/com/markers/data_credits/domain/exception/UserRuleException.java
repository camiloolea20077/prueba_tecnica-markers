package com.markers.data_credits.domain.exception;

/**
 * Regla de negocio de usuarios incumplida (rol inexistente, autoprotección, último administrador…). HTTP 422.
 */
public class UserRuleException extends DomainException {

    public UserRuleException(String message) {
        super(message);
    }
}
