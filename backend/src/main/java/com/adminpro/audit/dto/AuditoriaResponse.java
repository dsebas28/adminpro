package com.adminpro.audit.dto;

import java.time.Instant;
import java.util.Map;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.common.RefResponse;

public record AuditoriaResponse(
        Long id,
        String entidad,
        Long entidadId,
        AccionAuditoria accion,
        Map<String, Object> valorAnterior,
        Map<String, Object> valorNuevo,
        RefResponse usuario,
        String ip,
        Instant createdAt) {
}
