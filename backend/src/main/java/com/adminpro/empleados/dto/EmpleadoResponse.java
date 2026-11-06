package com.adminpro.empleados.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.adminpro.common.RefResponse;

public record EmpleadoResponse(
        Long id,
        String cedula,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        LocalDate fechaIngreso,
        RefResponse departamento,
        RefResponse cargo,
        boolean activo,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
