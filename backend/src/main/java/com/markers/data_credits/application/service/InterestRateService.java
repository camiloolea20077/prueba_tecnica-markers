package com.markers.data_credits.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.markers.data_credits.domain.model.CreditPolicy;
import com.markers.data_credits.domain.model.RateCatalog;
import com.markers.data_credits.domain.port.in.QueryInterestRatesUseCase;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;

/**
 * Consulta de tramos de tasa EA (el CRUD del admin llega en el módulo 3).
 */
@Service
@Transactional(readOnly = true)
public class InterestRateService implements QueryInterestRatesUseCase {

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
}
