package com.adminpro.usuarios.dto;

import java.time.Instant;

import com.adminpro.common.RefResponse;

public record UsuarioResponse(
        Long id,
        String correo,
        String nombreVisible,
        RefResponse rol,
        RefResponse empleado,
        boolean activo,
        Instant bloqueadoHasta,
        Instant ultimoAccesoAt,
        long version) {
}
