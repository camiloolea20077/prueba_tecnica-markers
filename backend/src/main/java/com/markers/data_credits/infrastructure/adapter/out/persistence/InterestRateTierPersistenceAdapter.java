package com.markers.data_credits.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.exception.InterestRateTierNotFoundException;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.InterestRateTierEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.mapper.CreditPersistenceMapper;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.InterestRateTierJpaRepository;

/**
 * Adaptador JPA del puerto {@link InterestRateTierRepositoryPort}.
 */
@Component
public class InterestRateTierPersistenceAdapter implements InterestRateTierRepositoryPort {

    private final InterestRateTierJpaRepository repository;
    private final CreditPersistenceMapper mapper;

    public InterestRateTierPersistenceAdapter(InterestRateTierJpaRepository repository,
                                              CreditPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<InterestRateTier> findActive() {
        return repository.findByActiveTrueOrderByMinTermMonthsAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<InterestRateTier> findActiveForTerm(int termMonths) {
        return repository.findActiveForTerm(termMonths).map(mapper::toDomain);
    }

    @Override
    public List<InterestRateTier> findAll() {
        return repository.findAllByOrderByMinTermMonthsAscIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<InterestRateTier> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public InterestRateTier save(InterestRateTier tier) {
        InterestRateTierEntity entity = tier.id() == null
                ? new InterestRateTierEntity()
                : repository.findById(tier.id()).orElseThrow(() -> new InterestRateTierNotFoundException(tier.id()));
        entity.setName(tier.name());
        entity.setMinTermMonths(tier.minTermMonths());
        entity.setMaxTermMonths(tier.maxTermMonths());
        entity.setAnnualEffectiveRate(tier.annualEffectiveRate());
        entity.setActive(tier.active());
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
