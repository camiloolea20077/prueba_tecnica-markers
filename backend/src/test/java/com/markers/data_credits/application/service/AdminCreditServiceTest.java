package com.markers.data_credits.application.service;

import static com.markers.data_credits.support.TestCredits.POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase.ApproveCreditCommand;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase.RejectCreditCommand;
import com.markers.data_credits.domain.port.in.SearchCreditsUseCase.CreditSearch;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;
import com.markers.data_credits.support.TestCredits;

@ExtendWith(MockitoExtension.class)
class AdminCreditServiceTest {

    @Mock
    private CreditRepositoryPort creditRepository;

    private AdminCreditService service;

    @BeforeEach
    void setUp() {
        service = new AdminCreditService(creditRepository, POLICY);
    }

    @Test
    @DisplayName("aprobar guarda el crédito APROBADO con las condiciones calculadas")
    void approve() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));
        when(creditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Credit result = service.approve(new ApproveCreditCommand(10L, 2L, new BigDecimal("21")));

        assertThat(result.status()).isEqualTo(CreditStatus.APPROVED);
        assertThat(result.annualEffectiveRate()).isEqualByComparingTo("21");
        assertThat(result.monthlyPayment()).isPositive();
    }

    @Test
    @DisplayName("aprobar un crédito ya decidido → 409 y no guarda")
    void approveAlreadyDecided() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.REJECTED)));

        assertThatThrownBy(() -> service.approve(new ApproveCreditCommand(10L, 2L, new BigDecimal("21"))))
                .isInstanceOf(InvalidCreditStateException.class);
        verify(creditRepository, never()).save(any());
    }

    @Test
    @DisplayName("aprobar con tasa sobre la usura → 422 y no guarda")
    void approveRateTooHigh() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));

        assertThatThrownBy(() -> service.approve(new ApproveCreditCommand(10L, 2L, new BigDecimal("35"))))
                .isInstanceOf(CreditRuleException.class);
        verify(creditRepository, never()).save(any());
    }

    @Test
    @DisplayName("decidir un crédito inexistente → 404")
    void decideMissing() {
        when(creditRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(new RejectCreditCommand(77L, 2L, "Motivo suficientemente largo")))
                .isInstanceOf(CreditNotFoundException.class);
    }

    @Test
    @DisplayName("rechazar guarda el motivo")
    void reject() {
        when(creditRepository.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));
        when(creditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Credit result = service.reject(new RejectCreditCommand(10L, 2L, "Historial crediticio negativo"));

        assertThat(result.status()).isEqualTo(CreditStatus.REJECTED);
        assertThat(result.rejectionReason()).isEqualTo("Historial crediticio negativo");
    }

    @Test
    @DisplayName("la búsqueda normaliza página, tamaño y texto")
    void searchNormalizesInput() {
        PageResult<Credit> empty = new PageResult<>(List.of(), 0, 100, 0);
        when(creditRepository.search(CreditStatus.PENDING, null, 0, 100)).thenReturn(empty);

        PageResult<Credit> result = service.search(new CreditSearch(CreditStatus.PENDING, "   ", -3, 5000));

        assertThat(result).isSameAs(empty);
    }

    @Test
    @DisplayName("el resumen incluye todos los estados, con 0 si no hay créditos")
    void summaryFillsMissingStatuses() {
        when(creditRepository.countByStatus()).thenReturn(Map.of(CreditStatus.PENDING, 4L));

        Map<CreditStatus, Long> counts = service.countByStatus();

        assertThat(counts).containsEntry(CreditStatus.PENDING, 4L)
                .containsEntry(CreditStatus.APPROVED, 0L)
                .containsEntry(CreditStatus.REJECTED, 0L)
                .containsEntry(CreditStatus.CANCELLED, 0L);
    }
}
