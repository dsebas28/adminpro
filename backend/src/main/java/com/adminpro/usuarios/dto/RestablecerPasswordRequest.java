package com.adminpro.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank(message = "La contraseña temporal es obligatoria")
        @Size(min = 10, max = 72, message = "Debe tener entre 10 y 72 caracteres")
        String passwordTemporal) {
}
