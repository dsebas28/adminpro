package com.adminpro.dashboard.dto;

import java.time.Instant;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.documentos.entity.EstadoDocumento;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record ResumenResponse(long empleadosActivos, long empleadosInactivos, long documentosActivos,
            long documentosPorVencer, long documentosVencidos) {
    }

    public record ConteoEstadoResponse(EstadoDocumento estado, long total) {
    }

    public record ActividadResponse(AccionAuditoria accion, String entidad, Long entidadId, String etiqueta,
            String usuario, Instant fecha) {
    }

    public record AlertaResponse(String tipo, String severidad, String mensaje, String entidad, Long entidadId) {
    }
}
