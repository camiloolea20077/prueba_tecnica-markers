package com.markers.data_credits.support;

import java.math.BigDecimal;
import java.time.Instant;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditApplicant;
import com.markers.data_credits.domain.model.CreditPolicy;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.InterestRateTier;

/**
 * Datos de prueba de créditos (misma política que application.yml).
 */
public final class TestCredits {

    public static final CreditPolicy POLICY = new CreditPolicy(
            new BigDecimal("1000000"), new BigDecimal("200000000"), 6, 84, 3,
            new BigDecimal("10.0"), new BigDecimal("28.0"));

    public static final InterestRateTier MEDIUM_TIER =
            new InterestRateTier(2L, "Mediano plazo", 13, 36, new BigDecimal("22.0000"), true);

    public static final CreditApplicant APPLICANT = new CreditApplicant(1L, "Usuario Demo", "usuario@test.com");

    private TestCredits() {
    }

    /** Crédito persistido con el estado indicado, del usuario 1. */
    public static Credit credit(Long id, CreditStatus status) {
        return new Credit(id, APPLICANT, new BigDecimal("10000000.00"), 24, new BigDecimal("22.0000"),
                null, null, null, null, null, status, null, null, null, Instant.parse("2026-09-23T15:00:00Z"), 0L);
    }
}
