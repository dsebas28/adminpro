package com.adminpro.empleados.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.adminpro.common.Patrones;

public record EmpleadoRequest(
        @NotBlank(message = "La cédula es obligatoria")
        @Pattern(regexp = "^[0-9]{6,10}$", message = "Debe tener entre 6 y 10 dígitos")
        String cedula,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 150, message = "Máximo 150 caracteres")
        @Pattern(regexp = Patrones.CORREO, message = "El correo no es válido")
        String correo,

        @Pattern(regexp = "^[0-9]{10}$", message = "Debe tener 10 dígitos")
        String telefono,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        LocalDate fechaIngreso,

        @NotNull(message = "El departamento es obligatorio")
        Long departamentoId,

        @NotNull(message = "El cargo es obligatorio")
        Long cargoId,

        Long version) {
}
