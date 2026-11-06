package com.adminpro.audit.controller;

import java.time.OffsetDateTime;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.audit.dto.AuditoriaResponse;
import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditoriaConsultaService;
import com.adminpro.common.PageResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoría")
@PreAuthorize("hasAuthority('AUDITORIA_LEER')")
public class AuditoriaController {

    private final AuditoriaConsultaService servicio;

    public AuditoriaController(AuditoriaConsultaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PageResponse<AuditoriaResponse> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable paginacion) {
        return servicio.listar(entidad, entidadId, usuarioId, accion, desde, hasta, paginacion);
    }

    @GetMapping("/{id}")
    public AuditoriaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }
}
