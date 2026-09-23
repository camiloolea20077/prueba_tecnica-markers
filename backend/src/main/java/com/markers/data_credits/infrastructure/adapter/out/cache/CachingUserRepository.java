package com.markers.data_credits.infrastructure.adapter.out.cache;

import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.UserPersistenceAdapter;
import com.markers.data_credits.infrastructure.config.CacheConfig;

/**
 * Los usuarios no se cachean, pero los créditos cacheados incluyen nombre y correo del solicitante:
 * al editar o eliminar un usuario se vacían las cachés de créditos (tras el commit) para no mostrar datos viejos.
 */
@Primary
@Component
public class CachingUserRepository implements UserRepositoryPort {

    private final UserPersistenceAdapter delegate;
    private final CacheManager cacheManager;

    public CachingUserRepository(UserPersistenceAdapter delegate, CacheManager cacheManager) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return delegate.findByEmail(email);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return delegate.findByIdForUpdate(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public PageResult<User> search(String query, String roleCode, Boolean active, int page, int size) {
        return delegate.search(query, roleCode, active, page, size);
    }

    @Override
    public boolean existsByEmail(String email, Long excludeId) {
        return delegate.existsByEmail(email, excludeId);
    }

    @Override
    public long countActiveByRole(String roleCode) {
        return delegate.countActiveByRole(roleCode);
    }

    @Override
    public boolean hasCredits(Long userId) {
        return delegate.hasCredits(userId);
    }

    @Override
    public User save(User user) {
        User saved = delegate.save(user);
        if (user.id() != null) {
            AfterCommit.run(this::clearCreditCaches);
        }
        return saved;
    }

    @Override
    public void deleteById(Long id) {
        delegate.deleteById(id);
        AfterCommit.run(this::clearCreditCaches);
    }

    private void clearCreditCaches() {
        for (String name : new String[] {CacheConfig.CREDIT_BY_ID, CacheConfig.CREDITS_BY_USER}) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
