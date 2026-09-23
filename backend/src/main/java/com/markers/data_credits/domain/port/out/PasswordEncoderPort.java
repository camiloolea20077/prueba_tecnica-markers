package com.markers.data_credits.domain.port.out;

/**
 * Puerto de salida para cifrar y verificar contraseñas.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
