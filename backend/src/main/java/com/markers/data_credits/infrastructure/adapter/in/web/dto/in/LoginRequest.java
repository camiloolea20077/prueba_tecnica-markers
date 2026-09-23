package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de inicio de sesión.
 */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 100, message = "La contraseña no puede superar 100 caracteres")
        String password) {
}
