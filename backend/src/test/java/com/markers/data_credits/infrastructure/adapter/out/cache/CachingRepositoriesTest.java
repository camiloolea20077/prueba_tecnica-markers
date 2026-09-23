package com.markers.data_credits.infrastructure.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.CreditPersistenceAdapter;
import com.markers.data_credits.infrastructure.adapter.out.persistence.InterestRateTierPersistenceAdapter;
import com.markers.data_credits.infrastructure.config.CacheConfig;
import com.markers.data_credits.support.TestCredits;

/**
 * Caché real (EhCache 3 + ehcache.xml de producción) sobre un repositorio JPA simulado:
 * demuestra que las lecturas repetidas no llegan a la base de datos y que las escrituras invalidan.
 * Se inyectan los puertos (como los usan los servicios): el bean es el proxy de caché.
 */
@SpringBootTest(classes = {CacheConfig.class, CachingCreditRepository.class, CachingInterestRateTierRepository.class})
@ImportAutoConfiguration(CacheAutoConfiguration.class)
class CachingRepositoriesTest {

    @Autowired
    private CreditRepositoryPort credits;
    @Autowired
    private InterestRateTierRepositoryPort tiers;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CreditPersistenceAdapter creditDelegate;
    @MockitoBean
    private InterestRateTierPersistenceAdapter tierDelegate;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("usa EhCache vía JCache con las cachés declaradas en ehcache.xml")
    void ehcacheIsConfigured() {
        assertThat(cacheManager).isInstanceOf(JCacheCacheManager.class);
        assertThat(cacheManager.getCacheNames())
                .contains(CacheConfig.CREDIT_BY_ID, CacheConfig.CREDITS_BY_USER, CacheConfig.ACTIVE_RATE_TIERS);
    }

    @Test
    @DisplayName("consultar el estado de un crédito 3 veces consulta la base de datos una sola vez")
    void creditByIdIsCached() {
        when(creditDelegate.findById(10L)).thenReturn(Optional.of(TestCredits.credit(10L, CreditStatus.PENDING)));

        credits.findById(10L);
        credits.findById(10L);
        Optional<Credit> third = credits.findById(10L);

        assertThat(third).map(Credit::status).contains(CreditStatus.PENDING);
        verify(creditDelegate, times(1)).findById(10L);
    }

    @Test
    @DisplayName("un crédito inexistente no se cachea (se vuelve a consultar)")
    void missingCreditIsNotCached() {
        when(creditDelegate.findById(99L)).thenReturn(Optional.empty());

        credits.findById(99L);
        credits.findById(99L);

        verify(creditDelegate, times(2)).findById(99L);
    }

    @Test
    @DisplayName("'mis créditos' se cachea por usuario y por filtro de estado")
    void creditsByUserAreCachedPerFilter() {
        when(creditDelegate.findByUserId(any(), any())).thenReturn(List.of());

        credits.findByUserId(1L, null);
        credits.findByUserId(1L, null);
        credits.findByUserId(1L, CreditStatus.PENDING);
        credits.findByUserId(2L, null);

        verify(creditDelegate, times(1)).findByUserId(1L, null);
        verify(creditDelegate, times(1)).findByUserId(1L, CreditStatus.PENDING);
        verify(creditDelegate, times(1)).findByUserId(2L, null);
    }

    @Test
    @DisplayName("guardar un crédito (p. ej. aprobarlo) invalida su detalle y las listas de su solicitante")
    void saveEvictsCreditAndOwnerLists() {
        Credit pending = TestCredits.credit(10L, CreditStatus.PENDING);
        Credit approved = pending.approve(2L, new BigDecimal("20"), TestCredits.POLICY);
        when(creditDelegate.findById(10L)).thenReturn(Optional.of(pending), Optional.of(approved));
        when(creditDelegate.findByUserId(1L, CreditStatus.PENDING)).thenReturn(List.of(pending), List.of());
        when(creditDelegate.save(any())).thenReturn(approved);

        credits.findById(10L);
        credits.findByUserId(1L, CreditStatus.PENDING);
        credits.save(approved); // sin transacción activa → invalida de inmediato

        assertThat(credits.findById(10L)).map(Credit::status).contains(CreditStatus.APPROVED);
        assertThat(credits.findByUserId(1L, CreditStatus.PENDING)).isEmpty();
        verify(creditDelegate, times(2)).findById(10L);
        verify(creditDelegate, times(2)).findByUserId(1L, CreditStatus.PENDING);
    }

    @Test
    @DisplayName("la tasa sugerida por plazo sale de la caché de tramos activos")
    void activeTiersAreCached() {
        when(tierDelegate.findActive()).thenReturn(List.of(TestCredits.MEDIUM_TIER));

        assertThat(tiers.findActiveForTerm(24)).contains(TestCredits.MEDIUM_TIER);
        assertThat(tiers.findActiveForTerm(30)).contains(TestCredits.MEDIUM_TIER);
        assertThat(tiers.findActive()).hasSize(1);
        assertThat(tiers.findActiveForTerm(60)).isEmpty();

        verify(tierDelegate, times(1)).findActive();
    }

    @Test
    @DisplayName("crear o eliminar un tramo invalida la caché de tramos")
    void tierChangesEvict() {
        InterestRateTier longTier = new InterestRateTier(3L, "Largo", 37, 84, new BigDecimal("25"), true);
        when(tierDelegate.findActive()).thenReturn(List.of(TestCredits.MEDIUM_TIER),
                List.of(TestCredits.MEDIUM_TIER, longTier));
        when(tierDelegate.save(any())).thenReturn(longTier);

        assertThat(tiers.findActiveForTerm(60)).isEmpty();
        tiers.save(longTier);
        assertThat(tiers.findActiveForTerm(60)).contains(longTier);

        clearInvocations(tierDelegate);
        tiers.deleteById(3L);
        tiers.findActive();
        verify(tierDelegate, times(1)).findActive();
    }
}
