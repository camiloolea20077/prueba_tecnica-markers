package com.markers.data_credits.infrastructure.adapter.in.web.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edición de usuario. {@code password} es opcional: si no llega (o llega vacía) se conserva la actual.
 */
public record UpdateUserRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email,

        @NotBlank(message = "El rol es obligatorio")
        String role,

        Boolean active,

        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String password) {

    /** Una contraseña vacía equivale a "no cambiarla" (evita que @Size rechace el campo vacío). */
    public UpdateUserRequest {
        password = password == null || password.isBlank() ? null : password;
    }
}
