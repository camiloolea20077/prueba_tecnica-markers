package com.markers.data_credits.infrastructure.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Convierte un JWT válido en una {@link Authentication}.
 * <p>
 * Authorities: {@code ROLE_<rol>} + cada código de permiso. Así se puede usar
 * {@code hasRole('ADMIN')} o {@code hasAuthority('CREDIT_APPROVE')}.
 * Un token inválido produce {@code Mono.empty()} → la petición queda anónima.
 * </p>
 */
@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationManager(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = String.valueOf(authentication.getCredentials());
        return Mono.justOrEmpty(tokenProvider.parse(token))
                .map(user -> new UsernamePasswordAuthenticationToken(user, token, authorities(user)));
    }

    private static List<GrantedAuthority> authorities(AuthenticatedUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.role()));
        user.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }
}
