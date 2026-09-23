package com.markers.data_credits.infrastructure.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code data-credits.*} de application.yml.
 */
@ConfigurationProperties(prefix = "data-credits")
public record CreditProperties(Amount amount, Term term, int maxPendingPerUser, Rates rates) {

    public record Amount(BigDecimal min, BigDecimal max) {
    }

    public record Term(int minMonths, int maxMonths) {
    }

    public record Rates(BigDecimal minAnnual, BigDecimal maxAnnual) {
    }
}
