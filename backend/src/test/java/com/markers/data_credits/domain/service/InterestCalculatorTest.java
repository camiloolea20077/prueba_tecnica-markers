package com.markers.data_credits.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.markers.data_credits.domain.model.AmortizationRow;
import com.markers.data_credits.domain.model.CreditQuote;

class InterestCalculatorTest {

    private static final BigDecimal TEN_MILLION = new BigDecimal("10000000");

    @Test
    @DisplayName("24 % EA equivale a ≈ 1,8088 % mensual vencido")
    void annualToMonthly() {
        BigDecimal monthly = InterestCalculator.monthlyRateFraction(new BigDecimal("24"));

        assertThat(monthly.doubleValue()).isCloseTo(0.0180876, within(0.0000005));
    }

    @Test
    @DisplayName("componer la tasa mensual 12 veces devuelve la EA")
    void monthlyCompoundsBackToAnnual() {
        BigDecimal monthly = InterestCalculator.monthlyRateFraction(new BigDecimal("22"));

        double annual = Math.pow(1 + monthly.doubleValue(), 12) - 1;

        assertThat(annual).isCloseTo(0.22, within(1e-9));
    }

    @Test
    @DisplayName("cuota francesa: 10 M a 12 meses con 24 % EA ≈ 934.525")
    void fixedPayment() {
        CreditQuote quote = InterestCalculator.quote(TEN_MILLION, 12, new BigDecimal("24"), true);

        assertThat(quote.monthlyPayment().doubleValue()).isCloseTo(934_525, within(2.0));
        assertThat(quote.monthlyRate()).isEqualByComparingTo("1.808758");
        assertThat(quote.annualRate()).isEqualByComparingTo("24.0000");
    }

    @ParameterizedTest(name = "{0} a {1} meses con {2} % EA")
    @CsvSource({"10000000, 12, 24", "1000000, 6, 18", "200000000, 84, 25", "37500000.50, 36, 22"})
    @DisplayName("la tabla de amortización cuadra: abonos = capital, saldo final 0, totales consistentes")
    void scheduleIsConsistent(BigDecimal amount, int term, BigDecimal rate) {
        CreditQuote quote = InterestCalculator.quote(amount, term, rate, true);

        assertThat(quote.schedule()).hasSize(term);
        BigDecimal principalSum = quote.schedule().stream().map(AmortizationRow::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paymentSum = quote.schedule().stream().map(AmortizationRow::payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interestSum = quote.schedule().stream().map(AmortizationRow::interest)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(principalSum).isEqualByComparingTo(amount);
        assertThat(quote.schedule().getLast().balance()).isEqualByComparingTo("0");
        assertThat(quote.totalPayable()).isEqualByComparingTo(paymentSum);
        assertThat(quote.totalInterest()).isEqualByComparingTo(interestSum);
        assertThat(quote.totalPayable()).isEqualByComparingTo(quote.totalInterest().add(quote.amount()));
        // La última cuota solo difiere por redondeo
        assertThat(quote.schedule().getLast().payment().subtract(quote.monthlyPayment()).abs())
                .isLessThanOrEqualTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("la primera cuota paga más interés que la última (el interés decrece)")
    void interestDecreases() {
        CreditQuote quote = InterestCalculator.quote(TEN_MILLION, 24, new BigDecimal("22"), true);

        assertThat(quote.schedule().getFirst().interest())
                .isGreaterThan(quote.schedule().getLast().interest());
    }

    @Test
    @DisplayName("tasa 0 %: cuota = capital / plazo y sin intereses")
    void zeroRate() {
        CreditQuote quote = InterestCalculator.quote(new BigDecimal("1200000"), 12, BigDecimal.ZERO, false);

        assertThat(quote.monthlyPayment()).isEqualByComparingTo("100000.00");
        assertThat(quote.totalInterest()).isEqualByComparingTo("0");
        assertThat(quote.schedule()).isEmpty();
    }

    @Test
    @DisplayName("datos inválidos lanzan IllegalArgumentException")
    void invalidInput() {
        assertThatThrownBy(() -> InterestCalculator.quote(BigDecimal.ZERO, 12, BigDecimal.TEN, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterestCalculator.quote(TEN_MILLION, 0, BigDecimal.TEN, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InterestCalculator.quote(TEN_MILLION, 12, new BigDecimal("-1"), false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
