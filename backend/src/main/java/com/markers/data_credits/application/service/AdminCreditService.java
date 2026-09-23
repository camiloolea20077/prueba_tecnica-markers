package com.markers.data_credits.application.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditPolicy;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase;
import com.markers.data_credits.domain.port.in.SearchCreditsUseCase;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;

/**
 * Casos de uso del analista: consultar solicitudes y aprobarlas o rechazarlas.
 * <p>
 * Cada decisión es una transacción: carga → valida la transición → calcula condiciones → guarda.
 * Si otro analista decidió el mismo crédito en paralelo, el {@code @Version} de la entidad hace fallar
 * la segunda escritura (HTTP 409) y se revierte todo.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class AdminCreditService implements DecideCreditUseCase, SearchCreditsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final CreditRepositoryPort creditRepository;
    private final CreditPolicy policy;

    public AdminCreditService(CreditRepositoryPort creditRepository, CreditPolicy policy) {
        this.creditRepository = creditRepository;
        this.policy = policy;
    }

    @Override
    @Transactional
    public Credit approve(ApproveCreditCommand command) {
        Credit credit = load(command.creditId());
        return creditRepository.save(credit.approve(command.adminId(), command.annualRate(), policy));
    }

    @Override
    @Transactional
    public Credit reject(RejectCreditCommand command) {
        Credit credit = load(command.creditId());
        return creditRepository.save(credit.reject(command.adminId(), command.reason()));
    }

    @Override
    public PageResult<Credit> search(CreditSearch search) {
        int page = Math.max(search.page(), 0);
        int size = Math.clamp(search.size(), 1, MAX_PAGE_SIZE);
        String query = search.query() == null || search.query().isBlank() ? null : search.query().trim();
        return creditRepository.search(search.status(), query, page, size);
    }

    @Override
    public Map<CreditStatus, Long> countByStatus() {
        Map<CreditStatus, Long> counts = new EnumMap<>(CreditStatus.class);
        for (CreditStatus status : CreditStatus.values()) {
            counts.put(status, 0L);
        }
        counts.putAll(creditRepository.countByStatus());
        return counts;
    }

    private Credit load(Long creditId) {
        return creditRepository.findById(creditId).orElseThrow(() -> new CreditNotFoundException(creditId));
    }
}
