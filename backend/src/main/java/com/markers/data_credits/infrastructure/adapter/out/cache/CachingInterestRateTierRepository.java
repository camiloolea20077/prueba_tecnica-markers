package com.markers.data_credits.infrastructure.adapter.out.cache;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.InterestRateTierPersistenceAdapter;
import com.markers.data_credits.infrastructure.config.CacheConfig;

/**
 * Decorador con caché de los tramos de tasa. Los tramos activos se leen en cada simulación,
 * solicitud y consulta del catálogo, pero cambian muy poco: se cachea la lista completa.
 */
@Primary
@Component
public class CachingInterestRateTierRepository implements InterestRateTierRepositoryPort {

    private final InterestRateTierPersistenceAdapter delegate;
    private final CacheManager cacheManager;

    public CachingInterestRateTierRepository(InterestRateTierPersistenceAdapter delegate,
                                             CacheManager cacheManager) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.ACTIVE_RATE_TIERS, key = "'all'")
    public List<InterestRateTier> findActive() {
        return delegate.findActive();
    }

    /** Se resuelve sobre la lista activa cacheada (sin ir a la base de datos). */
    @Override
    public Optional<InterestRateTier> findActiveForTerm(int termMonths) {
        return activeTiers().stream().filter(t -> t.covers(termMonths)).findFirst();
    }

    @Override
    public List<InterestRateTier> findAll() {
        return delegate.findAll();
    }

    @Override
    public Optional<InterestRateTier> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public InterestRateTier save(InterestRateTier tier) {
        InterestRateTier saved = delegate.save(tier);
        AfterCommit.run(this::evictAll);
        return saved;
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
        AfterCommit.run(this::evictAll);
    }

    /** Lectura cacheada sin auto-invocación (que saltaría el proxy de {@code @Cacheable}). */
    private List<InterestRateTier> activeTiers() {
        return cache().get("all", delegate::findActive);
    }

    private void evictAll() {
        cache().clear();
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(CacheConfig.ACTIVE_RATE_TIERS);
        if (cache == null) {
            throw new IllegalStateException("No existe la caché " + CacheConfig.ACTIVE_RATE_TIERS);
        }
        return cache;
    }
}
