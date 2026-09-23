package com.markers.data_credits.application.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InactiveUserException;
import com.markers.data_credits.domain.exception.UserNotFoundException;
import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditApplicant;
import com.markers.data_credits.domain.model.CreditPolicy;
import com.markers.data_credits.domain.model.CreditQuote;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.CancelCreditUseCase;
import com.markers.data_credits.domain.port.in.QueryCreditUseCase;
import com.markers.data_credits.domain.port.in.RequestCreditUseCase;
import com.markers.data_credits.domain.port.in.SimulateCreditUseCase;
import com.markers.data_credits.domain.port.out.CreditRepositoryPort;
import com.markers.data_credits.domain.port.out.InterestRateTierRepositoryPort;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.domain.service.InterestCalculator;

/**
 * Casos de uso del solicitante: solicitar, simular, consultar y cancelar créditos.
 * Métodos síncronos (JPA); la capa web los ejecuta en {@code boundedElastic}.
 */
@Service
@Transactional(readOnly = true)
public class CreditService implements RequestCreditUseCase, SimulateCreditUseCase, QueryCreditUseCase,
        CancelCreditUseCase {

    private final CreditRepositoryPort creditRepository;
    private final InterestRateTierRepositoryPort tierRepository;
    private final UserRepositoryPort userRepository;
    private final CreditPolicy policy;

    public CreditService(CreditRepositoryPort creditRepository,
                         InterestRateTierRepositoryPort tierRepository,
                         UserRepositoryPort userRepository,
                         CreditPolicy policy) {
        this.creditRepository = creditRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.policy = policy;
    }

    /**
     * Registra la solicitud. Bloquea la fila del usuario para que dos solicitudes simultáneas
     * no superen el límite de pendientes.
     */
    @Override
    @Transactional
    public Credit request(RequestCreditCommand command) {
        Credit.validateAmountAndTerm(command.amount(), command.termMonths(), policy);

        User user = userRepository.findByIdForUpdate(command.userId())
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(command.userId())));
        if (!user.active()) {
            throw new InactiveUserException();
        }

        long pending = creditRepository.countByUserIdAndStatus(user.id(), CreditStatus.PENDING);
        if (pending >= policy.maxPendingPerUser()) {
            throw new CreditRuleException("Ya tienes " + pending
                    + " solicitudes pendientes; espera la respuesta antes de enviar otra");
        }

        InterestRateTier tier = findTier(command.termMonths());
        CreditApplicant applicant = new CreditApplicant(user.id(), user.fullName(), user.email());
        return creditRepository.save(Credit.request(applicant, command.amount(), command.termMonths(), tier, policy));
    }

    @Override
    public CreditQuote simulate(SimulateCreditCommand command) {
        Credit.validateAmountAndTerm(command.amount(), command.termMonths(), policy);

        BigDecimal rate = command.annualRate() != null
                ? command.annualRate()
                : findTier(command.termMonths()).annualEffectiveRate();
        if (!policy.isRateAllowed(rate)) {
            throw new CreditRuleException(policy.rateRangeMessage());
        }
        return InterestCalculator.quote(command.amount(), command.termMonths(), rate, command.includeSchedule());
    }

    @Override
    public List<Credit> findByUser(Long userId, CreditStatus status) {
        return creditRepository.findByUserId(userId, status);
    }

    @Override
    public Credit findById(Long creditId, Requester requester) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new CreditNotFoundException(creditId));
        if (!requester.canViewAll() && !credit.isOwnedBy(requester.userId())) {
            throw new CreditNotFoundException(creditId);
        }
        return credit;
    }

    @Override
    @Transactional
    public Credit cancel(Long creditId, Long userId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new CreditNotFoundException(creditId));
        return creditRepository.save(credit.cancel(userId));
    }

    private InterestRateTier findTier(int termMonths) {
        return tierRepository.findActiveForTerm(termMonths)
                .orElseThrow(() -> new CreditRuleException(
                        "No hay una tasa configurada para un plazo de " + termMonths + " meses"));
    }
}
