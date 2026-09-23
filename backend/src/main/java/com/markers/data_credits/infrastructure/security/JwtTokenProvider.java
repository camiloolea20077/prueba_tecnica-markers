package com.markers.data_credits.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.model.AuthToken;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.out.TokenProviderPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida JWT firmados con HS256.
 * <p>
 * Claims: {@code sub}=email, {@code uid}, {@code name}, {@code role}, {@code permissions}.
 * </p>
 */
@Component
public class JwtTokenProvider implements TokenProviderPort {

    static final String CLAIM_UID = "uid";
    static final String CLAIM_NAME = "name";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_PERMISSIONS = "permissions";

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final Duration expiration;

    public JwtTokenProvider(SecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secret()));
        this.expiration = Duration.ofMinutes(properties.jwt().expirationMinutes());
    }

    @Override
    public AuthToken generate(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(user.email())
                .claim(CLAIM_UID, user.id())
                .claim(CLAIM_NAME, user.fullName())
                .claim(CLAIM_ROLE, user.role().code())
                .claim(CLAIM_PERMISSIONS, List.copyOf(user.role().permissions()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new AuthToken(token, expiresAt);
    }

    /**
     * Valida firma y expiración y construye el principal.
     *
     * @return vacío si el token es inválido, está alterado o expiró
     */
    public Optional<AuthenticatedUser> parse(String token) {
        if (!hasCanonicalSegments(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(
                    claims.get(CLAIM_UID, Number.class).longValue(),
                    claims.getSubject(),
                    claims.get(CLAIM_NAME, String.class),
                    claims.get(CLAIM_ROLE, String.class),
                    toStringSet(claims.get(CLAIM_PERMISSIONS, Collection.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            log.debug("JWT rechazado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * El decodificador Base64URL de jjwt ignora un carácter sobrante al final de un segmento
     * (longitud % 4 == 1, no forma un byte). Se rechaza para no aceptar tokens distintos al emitido.
     */
    private static boolean hasCanonicalSegments(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] segments = token.split("\\.", -1);
        if (segments.length != 3) {
            return false;
        }
        for (String segment : segments) {
            if (segment.isEmpty() || segment.length() % 4 == 1) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> toStringSet(Collection<?> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
    }
}
