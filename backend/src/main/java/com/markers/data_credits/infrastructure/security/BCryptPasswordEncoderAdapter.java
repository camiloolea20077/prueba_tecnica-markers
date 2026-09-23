package com.markers.data_credits.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.markers.data_credits.domain.port.out.PasswordEncoderPort;

/**
 * Adaptador BCrypt del puerto {@link PasswordEncoderPort}.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordEncoderAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return rawPassword != null && encodedPassword != null && passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
