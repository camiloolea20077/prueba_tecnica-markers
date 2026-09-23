package com.markers.data_credits.domain.model;

import static com.markers.data_credits.support.TestCredits.APPLICANT;
import static com.markers.data_credits.support.TestCredits.MEDIUM_TIER;
import static com.markers.data_credits.support.TestCredits.POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.support.TestCredits;

class CreditTest {

    @Test
    @DisplayName("una solicitud válida nace PENDIENTE con la tasa sugerida del tramo")
    void requestValid() {
        Credit credit = Credit.request(APPLICANT, new BigDecimal("15000000"), 24, MEDIUM_TIER, POLICY);

        assertThat(credit.status()).isEqualTo(CreditStatus.PENDING);
        assertThat(credit.suggestedAnnualRate()).isEqualByComparingTo("22");
        assertThat(credit.annualEffectiveRate()).isNull();
        assertThat(credit.amount()).isEqualByComparingTo("15000000.00");
    }

    @ParameterizedTest(name = "monto {0} → rechazado")
    @CsvSource({"999999.99", "200000000.01", "0"})
    @DisplayName("monto fuera de la política → CreditRuleException")
    void requestInvalidAmount(BigDecimal amount) {
        assertThatThrownBy(() -> Credit.request(APPLICANT, amount, 24, MEDIUM_TIER, POLICY))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("monto");
    }

    @ParameterizedTest(name = "plazo {0} → rechazado")
    @CsvSource({"5", "85"})
    @DisplayName("plazo fuera de la política → CreditRuleException")
    void requestInvalidTerm(int term) {
        assertThatThrownBy(() -> Credit.request(APPLICANT, new BigDecimal("5000000"), term, MEDIUM_TIER, POLICY))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("plazo");
    }

    @Test
    @DisplayName("el tramo debe cubrir el plazo solicitado")
    void requestTierMustCoverTerm() {
        assertThatThrownBy(() -> Credit.request(APPLICANT, new BigDecimal("5000000"), 12, MEDIUM_TIER, POLICY))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("12 meses");
    }

    @Test
    @DisplayName("un pendiente muestra cuota estimada con la tasa sugerida")
    void pendingQuoteIsEstimated() {
        Credit credit = TestCredits.credit(10L, CreditStatus.PENDING);

        assertThat(credit.isEstimated()).isTrue();
        assertThat(credit.quote().annualRate()).isEqualByComparingTo("22");
        assertThat(credit.quote().monthlyPayment()).isPositive();
    }

    @Test
    @DisplayName("el dueño puede cancelar su solicitud pendiente")
    void cancelByOwner() {
        Credit cancelled = TestCredits.credit(10L, CreditStatus.PENDING).cancel(APPLICANT.id());

        assertThat(cancelled.status()).isEqualTo(CreditStatus.CANCELLED);
        assertThat(cancelled.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("otro usuario no puede cancelar (y no se revela que el crédito existe)")
    void cancelByStranger() {
        Credit credit = TestCredits.credit(10L, CreditStatus.PENDING);

        assertThatThrownBy(() -> credit.cancel(99L)).isInstanceOf(CreditNotFoundException.class);
    }

    @ParameterizedTest(name = "estado {0} → no se puede cancelar")
    @EnumSource(value = CreditStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    @DisplayName("solo se cancela lo que está pendiente")
    void cancelNotPending(CreditStatus status) {
        Credit credit = TestCredits.credit(10L, status);

        assertThatThrownBy(() -> credit.cancel(APPLICANT.id()))
                .isInstanceOf(InvalidCreditStateException.class)
                .hasMessageContaining("cancelar");
    }
}
