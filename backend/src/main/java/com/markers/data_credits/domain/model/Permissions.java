package com.markers.data_credits.domain.model;

/**
 * Códigos de permisos del sistema. Deben coincidir con la tabla {@code permissions}.
 */
public final class Permissions {

    public static final String CREDIT_REQUEST = "CREDIT_REQUEST";
    public static final String CREDIT_VIEW_OWN = "CREDIT_VIEW_OWN";
    public static final String CREDIT_CANCEL_OWN = "CREDIT_CANCEL_OWN";
    public static final String CREDIT_SIMULATE = "CREDIT_SIMULATE";
    public static final String CREDIT_VIEW_ALL = "CREDIT_VIEW_ALL";
    public static final String CREDIT_APPROVE = "CREDIT_APPROVE";
    public static final String CREDIT_REJECT = "CREDIT_REJECT";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String RATE_MANAGE = "RATE_MANAGE";

    private Permissions() {
    }
}
