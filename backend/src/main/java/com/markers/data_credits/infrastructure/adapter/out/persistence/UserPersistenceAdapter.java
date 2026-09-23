package com.markers.data_credits.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.exception.UserNotFoundException;
import com.markers.data_credits.domain.exception.UserRuleException;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.CreditJpaRepository;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.RoleJpaRepository;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.UserSpecifications;

/**
 * Adaptador JPA del puerto {@link UserRepositoryPort}.
 */
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final RoleJpaRepository roleRepository;
    private final CreditJpaRepository creditRepository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repository,
                                  RoleJpaRepository roleRepository,
                                  CreditJpaRepository creditRepository,
                                  UserPersistenceMapper mapper) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.creditRepository = creditRepository;
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

    @Override
    public Optional<User> findById(Long id) {
        return repository.findWithRoleById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<User> search(String query, String roleCode, Boolean active, int page, int size) {
        Specification<UserEntity> spec = Specification.allOf(
                UserSpecifications.matches(query),
                UserSpecifications.hasRole(roleCode),
                UserSpecifications.isActive(active));
        Page<UserEntity> result = repository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("id"))));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public boolean existsByEmail(String email, Long excludeId) {
        return excludeId == null
                ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, excludeId);
    }

    @Override
    public long countActiveByRole(String roleCode) {
        return repository.countByRoleCodeAndActiveTrue(roleCode);
    }

    @Override
    public boolean hasCredits(Long userId) {
        return creditRepository.existsByUserId(userId);
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.id() == null
                ? new UserEntity()
                : repository.findById(user.id()).orElseThrow(() -> new UserNotFoundException(String.valueOf(user.id())));
        entity.setFullName(user.fullName());
        entity.setEmail(user.email());
        entity.setPassword(user.passwordHash());
        entity.setActive(user.active());
        entity.setRole(roleRepository.findByCode(user.role().code())
                .orElseThrow(() -> new UserRuleException("El rol " + user.role().code() + " no existe")));
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
