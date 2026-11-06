package com.adminpro.documentos.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import org.springframework.web.multipart.MultipartFile;

import com.adminpro.common.PageResponse;
import com.adminpro.documentos.dto.DocumentoDtos.AdjuntoResponse;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoListItemResponse;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoRequest;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoResponse;
import com.adminpro.documentos.dto.DocumentoDtos.HistorialDocumentoResponse;
import com.adminpro.documentos.dto.DocumentoDtos.TransicionRequest;
import com.adminpro.documentos.entity.EstadoDocumento;
import com.adminpro.documentos.service.AdjuntoService;
import com.adminpro.documentos.service.DocumentoService;
import com.adminpro.documentos.service.FlujoDocumento.Accion;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos")
public class DocumentoController {

    private final DocumentoService servicio;
    private final AdjuntoService adjuntos;

    public DocumentoController(DocumentoService servicio, AdjuntoService adjuntos) {
        this.servicio = servicio;
        this.adjuntos = adjuntos;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENTOS_LEER')")
    public PageResponse<DocumentoListItemResponse> listar(
            @RequestParam(required = false) String consecutivo,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) Long responsableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate venceHasta,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject @PageableDefault(size = 20, sort = "consecutivo", direction = Sort.Direction.DESC)
            Pageable paginacion) {
        return servicio.listar(new DocumentoService.Filtros(consecutivo, q, categoriaId, estado, responsableId,
                fechaDesde, fechaHasta, venceHasta, activo), paginacion);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_LEER')")
    public DocumentoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENTOS_ESCRIBIR')")
    public ResponseEntity<DocumentoResponse> crear(@Valid @RequestBody DocumentoRequest solicitud) {
        DocumentoResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/documentos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_ESCRIBIR')")
    public DocumentoResponse actualizar(@PathVariable Long id, @Valid @RequestBody DocumentoRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAuthority('DOCUMENTOS_ESCRIBIR')")
    public DocumentoResponse enviar(@PathVariable Long id, @Valid @RequestBody TransicionRequest solicitud) {
        return servicio.transitar(id, Accion.ENVIAR, solicitud);
    }

    @PostMapping("/{id}/iniciar-revision")
    @PreAuthorize("hasAuthority('DOCUMENTOS_APROBAR')")
    public DocumentoResponse iniciarRevision(@PathVariable Long id, @Valid @RequestBody TransicionRequest solicitud) {
        return servicio.transitar(id, Accion.INICIAR_REVISION, solicitud);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('DOCUMENTOS_APROBAR')")
    public DocumentoResponse aprobar(@PathVariable Long id, @Valid @RequestBody TransicionRequest solicitud) {
        return servicio.transitar(id, Accion.APROBAR, solicitud);
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasAuthority('DOCUMENTOS_APROBAR')")
    public DocumentoResponse devolver(@PathVariable Long id, @Valid @RequestBody TransicionRequest solicitud) {
        return servicio.transitar(id, Accion.DEVOLVER, solicitud);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('DOCUMENTOS_APROBAR')")
    public DocumentoResponse finalizar(@PathVariable Long id, @Valid @RequestBody TransicionRequest solicitud) {
        return servicio.transitar(id, Accion.FINALIZAR, solicitud);
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('DOCUMENTOS_DESACTIVAR')")
    public DocumentoResponse activar(@PathVariable Long id) {
        return servicio.activar(id);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('DOCUMENTOS_DESACTIVAR')")
    public DocumentoResponse desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAuthority('DOCUMENTOS_LEER')")
    public PageResponse<HistorialDocumentoResponse> historial(@PathVariable Long id,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable paginacion) {
        return servicio.historial(id, paginacion);
    }

    @GetMapping("/{id}/adjuntos")
    @PreAuthorize("hasAuthority('DOCUMENTOS_LEER')")
    public List<AdjuntoResponse> listarAdjuntos(@PathVariable Long id) {
        return adjuntos.listar(id);
    }

    @PostMapping(path = "/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENTOS_ESCRIBIR')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjuntoResponse agregarAdjunto(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return adjuntos.agregar(id, archivo.getOriginalFilename(), archivo.getBytes());
    }

    /** Se fuerza la descarga para que un archivo nunca se ejecute dentro de la aplicación. */
    @GetMapping("/{id}/adjuntos/{adjuntoId}/descarga")
    @PreAuthorize("hasAuthority('DOCUMENTOS_LEER')")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, @PathVariable Long adjuntoId) {
        AdjuntoService.Descarga descarga = adjuntos.descargar(id, adjuntoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(descarga.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(descarga.archivo());
    }

    @DeleteMapping("/{id}/adjuntos/{adjuntoId}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_ESCRIBIR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarAdjunto(@PathVariable Long id, @PathVariable Long adjuntoId) {
        adjuntos.eliminar(id, adjuntoId);
    }
}
