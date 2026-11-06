package com.adminpro.common.catalogo;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.adminpro.common.PageResponse;

/** Endpoints comunes de los catálogos. La lectura depende del módulo al que pertenece cada uno. */
public abstract class CatalogoController<E extends CatalogoEntity> {

    private static final String LEER = "hasAuthority(#root.this.permisoLectura())";
    private static final String ESCRIBIR = "hasAuthority('CATALOGOS_ESCRIBIR')";

    private final CatalogoService<E> servicio;

    protected CatalogoController(CatalogoService<E> servicio) {
        this.servicio = servicio;
    }

    public abstract String permisoLectura();

    @GetMapping
    @PreAuthorize(LEER)
    public PageResponse<CatalogoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject @PageableDefault(size = 20, sort = "nombre") Pageable paginacion) {
        return servicio.listar(q, activo, paginacion);
    }

    @GetMapping("/{id}")
    @PreAuthorize(LEER)
    public CatalogoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize(ESCRIBIR)
    public ResponseEntity<CatalogoResponse> crear(@Valid @RequestBody CatalogoRequest solicitud,
            HttpServletRequest peticion) {
        CatalogoResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create(peticion.getRequestURI() + "/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRIBIR)
    public CatalogoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CatalogoRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize(ESCRIBIR)
    public CatalogoResponse activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(ESCRIBIR)
    public CatalogoResponse desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }
}
