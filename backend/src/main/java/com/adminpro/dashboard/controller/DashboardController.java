package com.adminpro.dashboard.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.common.PageResponse;
import com.adminpro.dashboard.dto.DashboardDtos.ActividadResponse;
import com.adminpro.dashboard.dto.DashboardDtos.AlertaResponse;
import com.adminpro.dashboard.dto.DashboardDtos.ConteoEstadoResponse;
import com.adminpro.dashboard.dto.DashboardDtos.ResumenResponse;
import com.adminpro.dashboard.service.DashboardService;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoListItemResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@PreAuthorize("hasAuthority('DASHBOARD_LEER')")
public class DashboardController {

    private final DashboardService servicio;

    public DashboardController(DashboardService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return servicio.resumen();
    }

    @GetMapping("/documentos-por-estado")
    public List<ConteoEstadoResponse> documentosPorEstado() {
        return servicio.documentosPorEstado();
    }

    @GetMapping("/documentos-por-vencer")
    public PageResponse<DocumentoListItemResponse> documentosPorVencer(
            @RequestParam(required = false) Integer dias,
            @ParameterObject @PageableDefault(size = 10) Pageable paginacion) {
        return servicio.documentosPorVencer(dias, paginacion);
    }

    @GetMapping("/actividad-reciente")
    public List<ActividadResponse> actividadReciente(@RequestParam(required = false) Integer limite) {
        return servicio.actividadReciente(limite);
    }

    @GetMapping("/alertas")
    public List<AlertaResponse> alertas() {
        return servicio.alertas();
    }
}
