package com.markers.data_credits.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activa la abstracción de caché de Spring sobre EhCache 3 (JCache).
 * Configuración de cachés en {@code resources/ehcache.xml} ({@code spring.cache.jcache.config}).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Detalle / estado de un crédito (clave: id). */
    public static final String CREDIT_BY_ID = "creditById";

    /** Créditos de un usuario (clave: {@code userId:STATUS|ALL}). */
    public static final String CREDITS_BY_USER = "creditsByUser";

    /** Tramos de tasa activos (clave: {@code all}). */
    public static final String ACTIVE_RATE_TIERS = "activeRateTiers";
}
