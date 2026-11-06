package com.adminpro.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombreVisible,

        @NotNull(message = "El rol es obligatorio")
        Long rolId,

        Long empleadoId,

        @NotNull(message = "La versión es obligatoria")
        Long version) {
}
