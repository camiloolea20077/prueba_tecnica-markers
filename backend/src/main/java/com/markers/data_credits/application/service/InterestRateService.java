package com.markers.data_credits.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InterestRateTierNotFoundException;
import com.markers.data_credits.domain.model.CreditPolicy;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.model.RateCatalog;
import com.markers.data_credits.domain.port.in.ManageInterestRatesUseCase;
import com.markers.data_credits.domain.port.in.QueryInterestRatesUseCase;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;

/**
 * Tramos de tasa EA: consulta pública (catálogo) y CRUD del administrador.
 */
@Service
@Transactional(readOnly = true)
public class InterestRateService implements QueryInterestRatesUseCase, ManageInterestRatesUseCase {

    private final InterestRateTierRepositoryPort tierRepository;
    private final CreditPolicy policy;

    public InterestRateService(InterestRateTierRepositoryPort tierRepository, CreditPolicy policy) {
        this.tierRepository = tierRepository;
        this.policy = policy;
    }

    @Override
    public RateCatalog getCatalog() {
        return RateCatalog.of(tierRepository.findActive(), policy);
    }

    @Override
    public List<InterestRateTier> findAll() {
        return tierRepository.findAll();
    }

    @Override
    @Transactional
    public InterestRateTier create(TierCommand command) {
        InterestRateTier tier = toTier(null, command);
        requireNoOverlap(tier);
        return tierRepository.save(tier);
    }

    @Override
    @Transactional
    public InterestRateTier update(Long id, TierCommand command) {
        tierRepository.findById(id).orElseThrow(() -> new InterestRateTierNotFoundException(id));
        InterestRateTier tier = toTier(id, command);
        requireNoOverlap(tier);
        return tierRepository.save(tier);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        tierRepository.findById(id).orElseThrow(() -> new InterestRateTierNotFoundException(id));
        tierRepository.deleteById(id);
    }

    private InterestRateTier toTier(Long id, TierCommand c) {
        return InterestRateTier.create(id, c.name(), c.minTermMonths(), c.maxTermMonths(), c.annualEffectiveRate(),
                c.isActive(), policy);
    }

    /** Dos tramos activos no pueden cubrir el mismo plazo (la tasa sugerida sería ambigua). */
    private void requireNoOverlap(InterestRateTier tier) {
        if (!tier.active()) {
            return;
        }
        tierRepository.findActive().stream()
                .filter(other -> !other.id().equals(tier.id()))
                .filter(tier::overlaps)
                .findFirst()
                .ifPresent(other -> {
                    throw new CreditRuleException(String.format(
                            "El rango %d–%d meses se cruza con el tramo \"%s\" (%d–%d meses)",
                            tier.minTermMonths(), tier.maxTermMonths(), other.name(),
                            other.minTermMonths(), other.maxTermMonths()));
                });
    }
}
