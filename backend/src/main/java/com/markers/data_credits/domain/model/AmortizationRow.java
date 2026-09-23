package com.markers.data_credits.domain.model;

import java.math.BigDecimal;

/**
 * Fila de la tabla de amortización (sistema francés).
 *
 * @param period    número de cuota (1..n)
 * @param payment   valor de la cuota
 * @param interest  porción de intereses
 * @param principal abono a capital
 * @param balance   saldo después del pago
 */
public record AmortizationRow(int period, BigDecimal payment, BigDecimal interest, BigDecimal principal,
                              BigDecimal balance) {
}
