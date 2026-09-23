package com.markers.data_credits.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.markers.data_credits.domain.model.CreditPolicy;

/**
 * Expone la política de créditos del dominio a partir de la configuración.
 */
@Configuration
@EnableConfigurationProperties(CreditProperties.class)
public class CreditPolicyConfig {

    @Bean
    public CreditPolicy creditPolicy(CreditProperties p) {
        return new CreditPolicy(
                p.amount().min(),
                p.amount().max(),
                p.term().minMonths(),
                p.term().maxMonths(),
                p.maxPendingPerUser(),
                p.rates().minAnnual(),
                p.rates().maxAnnual());
    }
}
