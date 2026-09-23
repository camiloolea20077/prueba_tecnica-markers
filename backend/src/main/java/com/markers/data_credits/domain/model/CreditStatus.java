package com.markers.data_credits.domain.model;

/**
 * Estados de un crédito. Transiciones válidas: PENDING → APPROVED | REJECTED | CANCELLED.
 */
public enum CreditStatus {
    PENDING("Pendiente"),
    APPROVED("Aprobado"),
    REJECTED("Rechazado"),
    CANCELLED("Cancelado");

    private final String label;

    CreditStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
