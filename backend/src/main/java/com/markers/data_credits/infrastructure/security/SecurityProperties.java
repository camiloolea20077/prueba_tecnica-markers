package com.markers.data_credits.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code security.*} de application.yml.
 *
 * @param jwt  secreto (Base64, mínimo 256 bits) y minutos de expiración del token
 * @param cors orígenes permitidos para el front-end
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(Jwt jwt, Cors cors) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
