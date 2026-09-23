package com.markers.data_credits.application.service;

import static com.markers.data_credits.support.TestCredits.POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InterestRateTierNotFoundException;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.port.in.ManageInterestRatesUseCase.TierCommand;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;

@ExtendWith(MockitoExtension.class)
class InterestRateServiceTest {

    private static final InterestRateTier SHORT = new InterestRateTier(1L, "Corto", 6, 12, new BigDecimal("18"), true);
    private static final InterestRateTier MEDIUM = new InterestRateTier(2L, "Mediano", 13, 36, new BigDecimal("22"), true);

    @Mock
    private InterestRateTierRepositoryPort repository;

    private InterestRateService service;

    @BeforeEach
    void setUp() {
        service = new InterestRateService(repository, POLICY);
    }

    private static TierCommand command(int min, int max, String rate) {
        return new TierCommand("  Nuevo tramo  ", min, max, new BigDecimal(rate), true);
    }

    @Test
    @DisplayName("crear un tramo que no se cruza lo guarda con el nombre normalizado")
    void createOk() {
        when(repository.findActive()).thenReturn(List.of(SHORT, MEDIUM));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InterestRateTier created = service.create(command(37, 84, "25"));

        assertThat(created.name()).isEqualTo("Nuevo tramo");
        assertThat(created.annualEffectiveRate()).isEqualByComparingTo("25.0000");
    }

    @Test
    @DisplayName("crear un tramo superpuesto con uno activo → 422 indicando con cuál")
    void createOverlap() {
        when(repository.findActive()).thenReturn(List.of(SHORT, MEDIUM));

        assertThatThrownBy(() -> service.create(command(30, 48, "24")))
                .isInstanceOf(CreditRuleException.class)
                .hasMessageContaining("Mediano");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un tramo inactivo puede superponerse (no afecta la tasa sugerida)")
    void inactiveMayOverlap() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InterestRateTier created = service.create(new TierCommand("Borrador", 6, 84, new BigDecimal("20"), false));

        assertThat(created.active()).isFalse();
    }

    @Test
    @DisplayName("editar un tramo no se compara consigo mismo")
    void updateSelfIsNotOverlap() {
        when(repository.findById(2L)).thenReturn(Optional.of(MEDIUM));
        when(repository.findActive()).thenReturn(List.of(SHORT, MEDIUM));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InterestRateTier updated = service.update(2L, command(13, 36, "21.5"));

        assertThat(updated.annualEffectiveRate()).isEqualByComparingTo("21.5");
    }

    @Test
    @DisplayName("datos fuera de la política → 422")
    void invalidData() {
        assertThatThrownBy(() -> service.create(command(24, 12, "20")))
                .isInstanceOf(CreditRuleException.class).hasMessageContaining("mínimo no puede ser mayor");
        assertThatThrownBy(() -> service.create(command(1, 12, "20")))
                .isInstanceOf(CreditRuleException.class).hasMessageContaining("entre 6 y 84");
        assertThatThrownBy(() -> service.create(command(6, 12, "30")))
                .isInstanceOf(CreditRuleException.class).hasMessageContaining("tasa efectiva anual");
    }

    @Test
    @DisplayName("editar o eliminar un tramo inexistente → 404")
    void missingTier() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, command(6, 12, "18")))
                .isInstanceOf(InterestRateTierNotFoundException.class);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(InterestRateTierNotFoundException.class);
    }
}
