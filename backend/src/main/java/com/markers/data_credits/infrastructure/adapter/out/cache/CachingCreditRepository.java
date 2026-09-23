package com.markers.data_credits.infrastructure.adapter.out.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.CreditPersistenceAdapter;
import com.markers.data_credits.infrastructure.config.CacheConfig;

/**
 * Decorador con caché del puerto {@link CreditRepositoryPort} (patrón Decorator).
 * <ul>
 * <li>Cachea las consultas repetitivas de estado: detalle por id y "mis créditos".</li>
 * <li>Cada escritura invalida, tras el commit, el crédito y las listas de su solicitante.</li>
 * <li>La búsqueda del administrador y los conteos no se cachean (cambian con cada decisión).</li>
 * </ul>
 * Es {@code @Primary}: los servicios reciben este bean sin saber que existe la caché.
 */
@Primary
@Component
public class CachingCreditRepository implements CreditRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(CachingCreditRepository.class);

    private final CreditPersistenceAdapter delegate;
    private final CacheManager cacheManager;

    public CachingCreditRepository(CreditPersistenceAdapter delegate, CacheManager cacheManager) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
    }

    /** Clave de "mis créditos": {@code userId:STATUS} o {@code userId:ALL}. */
    public static String userKey(Long userId, CreditStatus status) {
        return userId + ":" + (status == null ? "ALL" : status.name());
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CREDIT_BY_ID, key = "#id", unless = "#result == null")
    public Optional<Credit> findById(Long id) {
        log.debug("Caché sin dato: consultando crédito {} en base de datos", id);
        return delegate.findById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CREDITS_BY_USER,
            key = "T(com.markers.data_credits.infrastructure.adapter.out.cache.CachingCreditRepository).userKey(#userId, #status)")
    public List<Credit> findByUserId(Long userId, CreditStatus status) {
        log.debug("Caché sin dato: consultando créditos del usuario {} ({})", userId, status);
        return delegate.findByUserId(userId, status);
    }

    @Override
    public Credit save(Credit credit) {
        Credit saved = delegate.save(credit);
        AfterCommit.run(() -> evict(saved));
        return saved;
    }

    @Override
    public long countByUserIdAndStatus(Long userId, CreditStatus status) {
        return delegate.countByUserIdAndStatus(userId, status);
    }

    @Override
    public PageResult<Credit> search(CreditStatus status, String query, int page, int size) {
        return delegate.search(status, query, page, size);
    }

    @Override
    public Map<CreditStatus, Long> countByStatus() {
        return delegate.countByStatus();
    }

    private void evict(Credit credit) {
        cache(CacheConfig.CREDIT_BY_ID).evict(credit.id());
        Cache byUser = cache(CacheConfig.CREDITS_BY_USER);
        Long userId = credit.applicant().id();
        byUser.evict(userKey(userId, null));
        for (CreditStatus status : CreditStatus.values()) {
            byUser.evict(userKey(userId, status));
        }
        log.debug("Caché invalidada para el crédito {} y el usuario {}", credit.id(), userId);
    }

    private Cache cache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            throw new IllegalStateException("No existe la caché " + name + " (revisar ehcache.xml)");
        }
        return cache;
    }
}
