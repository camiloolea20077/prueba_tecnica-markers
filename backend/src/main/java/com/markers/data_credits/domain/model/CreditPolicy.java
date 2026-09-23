package com.markers.data_credits.domain.model;

import java.math.BigDecimal;

/**
 * Parámetros de negocio configurables (application.yml → {@code data-credits.*}).
 *
 * @param minAmount          monto mínimo (COP)
 * @param maxAmount          monto máximo (COP)
 * @param minTermMonths      plazo mínimo en meses
 * @param maxTermMonths      plazo máximo en meses
 * @param maxPendingPerUser  solicitudes pendientes simultáneas permitidas por usuario
 * @param minAnnualRate      tasa efectiva anual mínima aplicable (%)
 * @param maxAnnualRate      tasa efectiva anual máxima aplicable (%, tope de usura)
 */
public record CreditPolicy(BigDecimal minAmount,
                           BigDecimal maxAmount,
                           int minTermMonths,
                           int maxTermMonths,
                           int maxPendingPerUser,
                           BigDecimal minAnnualRate,
                           BigDecimal maxAnnualRate) {

    public boolean isAmountAllowed(BigDecimal amount) {
        return amount != null && amount.compareTo(minAmount) >= 0 && amount.compareTo(maxAmount) <= 0;
    }

    public boolean isTermAllowed(Integer termMonths) {
        return termMonths != null && termMonths >= minTermMonths && termMonths <= maxTermMonths;
    }

    public boolean isRateAllowed(BigDecimal annualRate) {
        return annualRate != null
                && annualRate.compareTo(minAnnualRate) >= 0
                && annualRate.compareTo(maxAnnualRate) <= 0;
    }

    public String rateRangeMessage() {
        return "La tasa efectiva anual debe estar entre " + minAnnualRate.stripTrailingZeros().toPlainString()
                + " % y " + maxAnnualRate.stripTrailingZeros().toPlainString() + " %";
    }
}
