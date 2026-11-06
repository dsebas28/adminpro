package com.adminpro.empleados.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.audit.dto.AuditoriaResponse;
import com.adminpro.common.PageResponse;
import com.adminpro.empleados.dto.EmpleadoRequest;
import com.adminpro.empleados.dto.EmpleadoResponse;
import com.adminpro.empleados.service.EmpleadoService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/empleados")
@Tag(name = "Empleados")
public class EmpleadoController {

    private final EmpleadoService servicio;

    public EmpleadoController(EmpleadoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLEADOS_LEER')")
    public PageResponse<EmpleadoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departamentoId,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject @PageableDefault(size = 20, sort = "apellidos") Pageable paginacion) {
        return servicio.listar(q, departamentoId, cargoId, activo, paginacion);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADOS_LEER')")
    public EmpleadoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLEADOS_ESCRIBIR')")
    public ResponseEntity<EmpleadoResponse> crear(@Valid @RequestBody EmpleadoRequest solicitud) {
        EmpleadoResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADOS_ESCRIBIR')")
    public EmpleadoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EmpleadoRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('EMPLEADOS_DESACTIVAR')")
    public EmpleadoResponse activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('EMPLEADOS_DESACTIVAR')")
    public EmpleadoResponse desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAuthority('EMPLEADOS_LEER')")
    public PageResponse<AuditoriaResponse> historial(@PathVariable Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable paginacion) {
        return servicio.historial(id, paginacion);
    }
}
