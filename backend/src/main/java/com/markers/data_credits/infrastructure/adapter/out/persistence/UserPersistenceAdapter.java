package com.markers.data_credits.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.UserJpaRepository;

/**
 * Adaptador JPA del puerto {@link UserRepositoryPort}.
 */
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }
}
