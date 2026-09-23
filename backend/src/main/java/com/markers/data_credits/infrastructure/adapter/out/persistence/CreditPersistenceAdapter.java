package com.markers.data_credits.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.CreditEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.mapper.CreditPersistenceMapper;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.CreditJpaRepository;
import com.markers.data_credits.infrastructure.adapter.out.persistence.repository.UserJpaRepository;

/**
 * Adaptador JPA del puerto {@link CreditRepositoryPort}.
 */
@Component
public class CreditPersistenceAdapter implements CreditRepositoryPort {

    private final CreditJpaRepository creditRepository;
    private final UserJpaRepository userRepository;
    private final CreditPersistenceMapper mapper;

    public CreditPersistenceAdapter(CreditJpaRepository creditRepository,
                                    UserJpaRepository userRepository,
                                    CreditPersistenceMapper mapper) {
        this.creditRepository = creditRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Inserta un crédito nuevo o actualiza el estado mutable de uno existente.
     * Monto, plazo, solicitante y tasa sugerida no cambian después de creado.
     */
    @Override
    public Credit save(Credit credit) {
        CreditEntity entity = credit.id() == null ? newEntity(credit) : existingEntity(credit);
        entity.setAnnualEffectiveRate(credit.annualEffectiveRate());
        entity.setMonthlyRate(credit.monthlyRate());
        entity.setMonthlyPayment(credit.monthlyPayment());
        entity.setTotalInterest(credit.totalInterest());
        entity.setTotalPayable(credit.totalPayable());
        entity.setStatus(credit.status());
        entity.setRejectionReason(credit.rejectionReason());
        entity.setDecidedBy(credit.decidedBy() == null ? null : userRepository.getReferenceById(credit.decidedBy()));
        entity.setDecidedAt(credit.decidedAt());
        return mapper.toDomain(creditRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<Credit> findById(Long id) {
        return creditRepository.findWithUserById(id).map(mapper::toDomain);
    }

    @Override
    public List<Credit> findByUserId(Long userId, CreditStatus status) {
        List<CreditEntity> entities = status == null
                ? creditRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : creditRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByUserIdAndStatus(Long userId, CreditStatus status) {
        return creditRepository.countByUserIdAndStatus(userId, status);
    }

    private CreditEntity newEntity(Credit credit) {
        CreditEntity entity = new CreditEntity();
        entity.setUser(userRepository.getReferenceById(credit.applicant().id()));
        entity.setAmount(credit.amount());
        entity.setTermMonths(credit.termMonths());
        entity.setSuggestedAnnualRate(credit.suggestedAnnualRate());
        return entity;
    }

    private CreditEntity existingEntity(Credit credit) {
        return creditRepository.findWithUserById(credit.id())
                .orElseThrow(() -> new CreditNotFoundException(credit.id()));
    }
}
