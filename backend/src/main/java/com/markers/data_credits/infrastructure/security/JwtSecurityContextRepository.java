package com.markers.data_credits.infrastructure.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Carga el {@link SecurityContext} desde la cabecera {@code Authorization: Bearer <jwt>}.
 * API stateless: nunca guarda sesión.
 */
@Component
public class JwtSecurityContextRepository implements ServerSecurityContextRepository {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAuthenticationManager authenticationManager;

    public JwtSecurityContextRepository(JwtAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(token, token))
                .map(SecurityContextImpl::new);
    }
}
