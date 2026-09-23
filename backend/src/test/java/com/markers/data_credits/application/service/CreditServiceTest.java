package com.markers.data_credits.application.service;

import static com.markers.data_credits.support.TestCredits.MEDIUM_TIER;
import static com.markers.data_credits.support.TestCredits.POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditQuote;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.port.in.QueryCreditUseCase.Requester;
import com.markers.data_credits.domain.port.in.RequestCreditUseCase.RequestCreditCommand;
import com.markers.data_credits.domain.port.in.SimulateCreditUseCase.SimulateCreditCommand;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.support.TestCredits;
import com.markers.data_credits.support.TestUsers;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private CreditRepositoryPort creditRepository;
    @Mock
    private InterestRateTierRepositoryPort tierRepository;
    @Mock
    private UserRepositoryPort userRepository;

    private CreditService service;

    @BeforeEach
    void setUp() {
        service = new CreditService(creditRepository, tierRepository, userRepository, POLICY);
    }

    // ---------- Solicitar ----------

    @Test
    @DisplayName("solicitud válida: bloquea al usuario, asigna la tasa del tramo y guarda PENDIENTE")
    void requestOk() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(creditRepository.countByUserIdAndStatus(1L, CreditStatus.PENDING)).thenReturn(1L);
        when(tierRepository.findActiveForTerm(24)).thenReturn(Optional.of(MEDIUM_TIER));
        when(creditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Credit credit = service.request(new RequestCreditCommand(1L, new BigDecimal("8000000"), 24));

        ArgumentCaptor<Credit> saved = ArgumentCaptor.forClass(Credit.class);
        verify(creditRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(CreditStatus.PENDING);
        assertThat(saved.getValue().suggestedAnnualRate()).isEqualByComparingTo("22");
        assertThat(saved.getValue().applicant().email()).isEqualTo("usuario@test.com");
        assertThat(credit.isEstimated()).isTrue();
    }

    @Test
    @DisplayName("con 3 solicitudes pendientes no permite otra y no guarda")
    void requestPendingLimit() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(creditRepository.countByUserIdAndStatus(1L, CreditStatus.PENDING)).thenReturn(3L);

        assertThatThrownBy(() -> service.request(new RequestCreditCommand(1L, new BigDecimal("8000000"), 24)))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("3 solicitudes pendientes");
        verify(creditRepository, never()).save(any());
    }

    @Test
    @DisplayName("monto fuera de rango se rechaza antes de tocar la base de datos")
    void requestInvalidAmountFailsFast() {
        assertThatThrownBy(() -> service.request(new RequestCreditCommand(1L, new BigDecimal("500"), 24)))
                .isInstanceOf(CreditRuleException.class);
        verifyNoInteractions(userRepository, creditRepository, tierRepository);
    }

    @Test
    @DisplayName("sin tramo de tasa para el plazo → CreditRuleException")
    void requestWithoutTier() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(tierRepository.findActiveForTerm(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(new RequestCreditCommand(1L, new BigDecimal("8000000"), 24)))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("No hay una tasa configurada");
    }

    // ---------- Simular ----------

    @Test
    @DisplayName("simulación sin tasa usa la del tramo")
    void simulateWithTierRate() {
        when(tierRepository.findActiveForTerm(24)).thenReturn(Optional.of(MEDIUM_TIER));

        CreditQuote quote = service.simulate(new SimulateCreditCommand(new BigDecimal("8000000"), 24, null, true));

        assertThat(quote.annualRate()).isEqualByComparingTo("22");
        assertThat(quote.schedule()).hasSize(24);
    }

    @Test
    @DisplayName("simulación con tasa propia no consulta tramos")
    void simulateWithCustomRate() {
        CreditQuote quote = service.simulate(
                new SimulateCreditCommand(new BigDecimal("8000000"), 24, new BigDecimal("15.5"), false));

        assertThat(quote.annualRate()).isEqualByComparingTo("15.5");
        assertThat(quote.schedule()).isEmpty();
        verifyNoInteractions(tierRepository);
    }

    @Test
    @DisplayName("simulación con tasa sobre el tope de usura → CreditRuleException")
    void simulateRateAboveMax() {
        assertThatThrownBy(() -> service.simulate(
                new SimulateCreditCommand(new BigDecimal("8000000"), 24, new BigDecimal("30"), false)))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("entre 10 % y 28 %");
    }

    // ---------- Consultar ----------

    @Test
    @DisplayName("el dueño ve su crédito")
    void findByIdOwner() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));

        assertThat(service.findById(10L, new Requester(1L, false)).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("otro usuario recibe 'no existe' aunque el crédito exista")
    void findByIdStranger() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));

        assertThatThrownBy(() -> service.findById(10L, new Requester(99L, false)))
                .isInstanceOf(CreditNotFoundException.class);
    }

    @Test
    @DisplayName("quien puede ver todos (admin) ve créditos ajenos")
    void findByIdAdmin() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));

        assertThat(service.findById(10L, new Requester(2L, true)).id()).isEqualTo(10L);
    }

    // ---------- Cancelar ----------

    @Test
    @DisplayName("cancelar un pendiente propio lo guarda como CANCELADO")
    void cancelOk() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));
        when(creditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.cancel(10L, 1L).status()).isEqualTo(CreditStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelar un aprobado → InvalidCreditStateException y no guarda")
    void cancelApproved() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.APPROVED)));

        assertThatThrownBy(() -> service.cancel(10L, 1L)).isInstanceOf(InvalidCreditStateException.class);
        verify(creditRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar un crédito inexistente → CreditNotFoundException")
    void cancelMissing() {
        when(creditRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(77L, 1L)).isInstanceOf(CreditNotFoundException.class);
    }
}
