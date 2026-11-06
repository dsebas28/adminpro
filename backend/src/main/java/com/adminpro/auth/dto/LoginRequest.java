package com.adminpro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password) {
}
