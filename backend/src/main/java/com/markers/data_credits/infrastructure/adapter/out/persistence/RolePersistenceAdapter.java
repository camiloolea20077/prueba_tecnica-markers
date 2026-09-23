package com.markers.data_credits.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.port.out.RoleRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.RoleJpaRepository;

/**
 * Adaptador JPA del puerto {@link RoleRepositoryPort}.
 */
@Component
public class RolePersistenceAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public RolePersistenceAdapter(RoleJpaRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Role> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }
}
