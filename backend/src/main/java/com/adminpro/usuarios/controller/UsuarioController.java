package com.adminpro.usuarios.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.common.PageResponse;
import com.adminpro.usuarios.dto.RestablecerPasswordRequest;
import com.adminpro.usuarios.dto.RolResponse;
import com.adminpro.usuarios.dto.UsuarioCreateRequest;
import com.adminpro.usuarios.dto.UsuarioResponse;
import com.adminpro.usuarios.dto.UsuarioUpdateRequest;
import com.adminpro.usuarios.service.UsuarioService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Usuarios y roles")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIOS_LEER')")
    public PageResponse<UsuarioResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject @PageableDefault(size = 20, sort = "nombreVisible") Pageable paginacion) {
        return servicio.listar(q, rolId, activo, paginacion);
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_LEER')")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIOS_ESCRIBIR')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCreateRequest solicitud) {
        UsuarioResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ESCRIBIR')")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @PostMapping("/usuarios/{id}/restablecer-password")
    @PreAuthorize("hasAuthority('USUARIOS_ESCRIBIR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerPassword(@PathVariable Long id, @Valid @RequestBody RestablecerPasswordRequest solicitud) {
        servicio.restablecerPassword(id, solicitud);
    }

    @PatchMapping("/usuarios/{id}/activar")
    @PreAuthorize("hasAuthority('USUARIOS_DESACTIVAR')")
    public UsuarioResponse activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PatchMapping("/usuarios/{id}/desactivar")
    @PreAuthorize("hasAuthority('USUARIOS_DESACTIVAR')")
    public UsuarioResponse desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('USUARIOS_LEER')")
    public List<RolResponse> listarRoles() {
        return servicio.listarRoles();
    }
}
