package com.markers.data_credits.domain.model;

import static com.markers.data_credits.support.TestCredits.POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.service.InterestCalculator;
import com.markers.data_credits.support.TestCredits;

/**
 * Aprobación y rechazo de créditos (reglas del analista).
 */
class CreditDecisionTest {

    private static final Long ADMIN_ID = 2L;

    @Test
    @DisplayName("aprobar fija la tasa EA del analista y calcula condiciones definitivas")
    void approve() {
        Credit pending = TestCredits.credit(10L, CreditStatus.PENDING);

        Credit approved = pending.approve(ADMIN_ID, new BigDecimal("19.5"), POLICY);

        CreditQuote expected = InterestCalculator.quote(pending.amount(), pending.termMonths(),
                new BigDecimal("19.5"), false);
        assertThat(approved.status()).isEqualTo(CreditStatus.APPROVED);
        assertThat(approved.annualEffectiveRate()).isEqualByComparingTo("19.5");
        assertThat(approved.monthlyRate()).isEqualByComparingTo(expected.monthlyRate());
        assertThat(approved.monthlyPayment()).isEqualByComparingTo(expected.monthlyPayment());
        assertThat(approved.totalPayable()).isEqualByComparingTo(expected.totalPayable());
        assertThat(approved.decidedBy()).isEqualTo(ADMIN_ID);
        assertThat(approved.decidedAt()).isNotNull();
        assertThat(approved.isEstimated()).isFalse();
        assertThat(approved.quote().monthlyPayment()).isEqualByComparingTo(expected.monthlyPayment());
        // La tasa sugerida se conserva para trazabilidad
        assertThat(approved.suggestedAnnualRate()).isEqualByComparingTo("22");
        assertThat(approved.version()).isEqualTo(pending.version());
    }

    @ParameterizedTest(name = "tasa {0} % → fuera de la política")
    @ValueSource(strings = {"9.99", "28.01", "40"})
    @DisplayName("aprobar con tasa fuera de rango → CreditRuleException")
    void approveRateOutOfRange(String rate) {
        Credit pending = TestCredits.credit(10L, CreditStatus.PENDING);

        assertThatThrownBy(() -> pending.approve(ADMIN_ID, new BigDecimal(rate), POLICY))
                .isInstanceOf(CreditRuleException.class)
                .hasMessage("La tasa efectiva anual debe estar entre 10 % y 28 %");
    }

    @ParameterizedTest(name = "estado {0} → no se puede aprobar ni rechazar")
    @EnumSource(value = CreditStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    @DisplayName("solo se decide sobre créditos pendientes")
    void decideNotPending(CreditStatus status) {
        Credit credit = TestCredits.credit(10L, status);

        assertThatThrownBy(() -> credit.approve(ADMIN_ID, new BigDecimal("20"), POLICY))
                .isInstanceOf(InvalidCreditStateException.class).hasMessageContaining("aprobar");
        assertThatThrownBy(() -> credit.reject(ADMIN_ID, "Motivo suficientemente largo"))
                .isInstanceOf(InvalidCreditStateException.class).hasMessageContaining("rechazar");
    }

    @Test
    @DisplayName("nadie decide sobre su propia solicitud")
    void noSelfDecision() {
        Credit pending = TestCredits.credit(10L, CreditStatus.PENDING);
        Long applicantId = pending.applicant().id();

        assertThatThrownBy(() -> pending.approve(applicantId, new BigDecimal("20"), POLICY))
                .isInstanceOf(CreditRuleException.class).hasMessageContaining("propia");
    }

    @Test
    @DisplayName("rechazar guarda el motivo sin espacios sobrantes y limpia condiciones")
    void reject() {
        Credit rejected = TestCredits.credit(10L, CreditStatus.PENDING)
                .reject(ADMIN_ID, "   Capacidad de pago insuficiente   ");

        assertThat(rejected.status()).isEqualTo(CreditStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Capacidad de pago insuficiente");
        assertThat(rejected.annualEffectiveRate()).isNull();
        assertThat(rejected.decidedBy()).isEqualTo(ADMIN_ID);
        assertThat(rejected.decidedAt()).isNotNull();
    }

    @ParameterizedTest(name = "motivo \"{0}\" → inválido")
    @ValueSource(strings = {"", "   ", "corto"})
    @DisplayName("rechazar exige un motivo de al menos 10 caracteres")
    void rejectInvalidReason(String reason) {
        Credit pending = TestCredits.credit(10L, CreditStatus.PENDING);

        assertThatThrownBy(() -> pending.reject(ADMIN_ID, reason))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("entre 10 y 500");
    }
}
