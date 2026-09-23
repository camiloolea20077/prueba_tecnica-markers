package com.markers.data_credits.domain.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.markers.data_credits.domain.model.AmortizationRow;
import com.markers.data_credits.domain.model.CreditQuote;

/**
 * Cálculos financieros de un crédito de cuota fija (sistema francés) con tasa efectiva anual.
 * <ul>
 * <li>Tasa mensual vencida equivalente: {@code i = (1 + EA)^(1/12) − 1}</li>
 * <li>Cuota: {@code C = P · i / (1 − (1 + i)^−n)} (si {@code i = 0}: {@code C = P / n})</li>
 * </ul>
 * Los totales salen de la tabla de amortización: la última cuota absorbe el redondeo,
 * así la suma de abonos a capital es exactamente el monto prestado.
 * Clase pura (sin Spring).
 */
public final class InterestCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    private InterestCalculator() {
    }

    /**
     * Convierte una tasa efectiva anual a tasa mensual vencida equivalente.
     *
     * @param annualRatePercent tasa EA en porcentaje (24 = 24 %)
     * @return tasa mensual como fracción (0.018088… para 24 % EA)
     */
    public static BigDecimal monthlyRateFraction(BigDecimal annualRatePercent) {
        requirePositiveOrZero(annualRatePercent, "La tasa efectiva anual");
        double ea = annualRatePercent.divide(HUNDRED, MC).doubleValue();
        double monthly = Math.pow(1 + ea, 1.0 / 12) - 1;
        return new BigDecimal(monthly, MC).setScale(12, RoundingMode.HALF_UP);
    }

    /**
     * Calcula cuota, tasa mensual, intereses y total a pagar.
     *
     * @param amount            capital prestado
     * @param termMonths        número de cuotas mensuales
     * @param annualRatePercent tasa efectiva anual (%)
     * @param includeSchedule   si se incluye la tabla de amortización en el resultado
     */
    public static CreditQuote quote(BigDecimal amount, int termMonths, BigDecimal annualRatePercent,
                                    boolean includeSchedule) {
        requirePositive(amount, "El monto");
        if (termMonths <= 0) {
            throw new IllegalArgumentException("El plazo debe ser mayor a cero");
        }

        BigDecimal principalAmount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal i = monthlyRateFraction(annualRatePercent);
        BigDecimal payment = fixedPayment(principalAmount, i, termMonths);

        List<AmortizationRow> rows = new ArrayList<>(termMonths);
        BigDecimal balance = principalAmount;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (int period = 1; period <= termMonths; period++) {
            BigDecimal interest = balance.multiply(i, MC).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal principal;
            BigDecimal rowPayment;
            if (period == termMonths) {
                principal = balance;
                rowPayment = principal.add(interest);
            } else {
                principal = payment.subtract(interest);
                rowPayment = payment;
            }
            balance = balance.subtract(principal);
            totalPaid = totalPaid.add(rowPayment);
            rows.add(new AmortizationRow(period, rowPayment, interest, principal, balance));
        }

        return new CreditQuote(
                principalAmount,
                termMonths,
                annualRatePercent.setScale(4, RoundingMode.HALF_UP),
                i.multiply(HUNDRED).setScale(RATE_SCALE, RoundingMode.HALF_UP),
                payment,
                totalPaid.subtract(principalAmount),
                totalPaid,
                includeSchedule ? rows : List.of());
    }

    private static BigDecimal fixedPayment(BigDecimal principal, BigDecimal i, int n) {
        if (i.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(n), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(i).pow(n, MC);            // (1 + i)^n
        BigDecimal numerator = principal.multiply(i, MC).multiply(factor, MC);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + " debe ser mayor a cero");
        }
    }

    private static void requirePositiveOrZero(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + " no puede ser negativa");
        }
    }
}
