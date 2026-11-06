package com.adminpro.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.adminpro.common.Patrones;

public record UsuarioCreateRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        @Pattern(regexp = Patrones.CORREO, message = "El correo no es válido")
        String correo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombreVisible,

        @NotNull(message = "El rol es obligatorio")
        Long rolId,

        Long empleadoId,

        @NotBlank(message = "La contraseña temporal es obligatoria")
        @Size(min = 10, max = 72, message = "Debe tener entre 10 y 72 caracteres")
        String passwordTemporal) {
}
