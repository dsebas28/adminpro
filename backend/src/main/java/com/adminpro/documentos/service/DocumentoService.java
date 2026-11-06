package com.adminpro.documentos.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.common.Consultas;
import com.adminpro.common.PageResponse;
import com.adminpro.common.RefResponse;
import com.adminpro.common.Zona;
import com.adminpro.documentos.dto.DocumentoDtos.AdjuntoResponse;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoListItemResponse;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoRequest;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoResponse;
import com.adminpro.documentos.dto.DocumentoDtos.HistorialDocumentoResponse;
import com.adminpro.documentos.dto.DocumentoDtos.TransicionRequest;
import com.adminpro.documentos.entity.AccionHistorial;
import com.adminpro.documentos.entity.CategoriaDocumento;
import com.adminpro.documentos.entity.Documento;
import com.adminpro.documentos.entity.DocumentoAdjunto;
import com.adminpro.documentos.entity.DocumentoHistorial;
import com.adminpro.documentos.entity.EstadoDocumento;
import com.adminpro.documentos.repository.CategoriaDocumentoRepository;
import com.adminpro.documentos.repository.DocumentoAdjuntoRepository;
import com.adminpro.documentos.repository.DocumentoHistorialRepository;
import com.adminpro.documentos.repository.DocumentoRepository;
import com.adminpro.documentos.service.FlujoDocumento.Accion;
import com.adminpro.empleados.entity.Empleado;
import com.adminpro.empleados.repository.EmpleadoRepository;
import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;
import com.adminpro.security.UsuarioActual;
import com.adminpro.usuarios.repository.UsuarioRepository;

import jakarta.persistence.criteria.Expression;

@Service
@Transactional(readOnly = true)
public class DocumentoService {

    private static final String ENTIDAD = "DOCUMENTO";
    private static final Set<String> ORDEN_PERMITIDO =
            Set.of("consecutivo", "titulo", "estado", "fechaDocumento", "fechaVencimiento");

    private final DocumentoRepository documentos;
    private final DocumentoAdjuntoRepository adjuntos;
    private final DocumentoHistorialRepository historial;
    private final CategoriaDocumentoRepository categorias;
    private final EmpleadoRepository empleados;
    private final UsuarioRepository usuarios;
    private final ConsecutivoService consecutivos;
    private final AuditService auditoria;
    private final Clock reloj;

    public DocumentoService(DocumentoRepository documentos, DocumentoAdjuntoRepository adjuntos,
            DocumentoHistorialRepository historial, CategoriaDocumentoRepository categorias,
            EmpleadoRepository empleados, UsuarioRepository usuarios, ConsecutivoService consecutivos,
            AuditService auditoria, Clock reloj) {
        this.documentos = documentos;
        this.adjuntos = adjuntos;
        this.historial = historial;
        this.categorias = categorias;
        this.empleados = empleados;
        this.usuarios = usuarios;
        this.consecutivos = consecutivos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public record Filtros(String consecutivo, String q, Long categoriaId, EstadoDocumento estado, Long responsableId,
            LocalDate fechaDesde, LocalDate fechaHasta, LocalDate venceHasta, Boolean activo) {
    }

    public PageResponse<DocumentoListItemResponse> listar(Filtros f, Pageable paginacion) {
        Consultas.validarOrden(paginacion, ORDEN_PERMITIDO);

        List<Specification<Documento>> filtros = new ArrayList<>();
        if (f.consecutivo() != null && !f.consecutivo().isBlank()) {
            filtros.add((raiz, consulta, cb) -> cb.like(cb.upper(raiz.get("consecutivo")),
                    Consultas.patronContiene(f.consecutivo().toUpperCase()), '\\'));
        }
        if (f.q() != null && !f.q().isBlank()) {
            // Repite la expresión del índice ix_documentos_busqueda para poder usarlo.
            filtros.add((raiz, consulta, cb) -> {
                Expression<String> texto = cb.concat(cb.concat(raiz.<String>get("consecutivo"), " "),
                        raiz.<String>get("titulo"));
                return cb.like(cb.function("f_normalizar", String.class, texto),
                        cb.function("f_normalizar", String.class, cb.literal(Consultas.patronContiene(f.q()))), '\\');
            });
        }
        if (f.categoriaId() != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("categoria").get("id"), f.categoriaId()));
        }
        if (f.estado() != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), f.estado()));
        }
        if (f.responsableId() != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("responsable").get("id"), f.responsableId()));
        }
        if (f.fechaDesde() != null) {
            filtros.add((raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get("fechaDocumento"), f.fechaDesde()));
        }
        if (f.fechaHasta() != null) {
            filtros.add((raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("fechaDocumento"), f.fechaHasta()));
        }
        if (f.venceHasta() != null) {
            filtros.add((raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("fechaVencimiento"), f.venceHasta()));
        }
        if (f.activo() != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("activo"), f.activo()));
        }
        return PageResponse.of(documentos.findAll(Specification.allOf(filtros), paginacion), this::aItem);
    }

    public DocumentoResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public PageResponse<HistorialDocumentoResponse> historial(Long id, Pageable paginacion) {
        buscar(id);
        Page<DocumentoHistorial> pagina = historial.findByDocumentoId(id, paginacion);
        Set<Long> autores = pagina.getContent().stream().map(DocumentoHistorial::getCreatedBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, RefResponse> porId = usuarios.findAllById(autores).stream()
                .map(u -> new RefResponse(u.getId(), u.getNombreVisible()))
                .collect(Collectors.toMap(RefResponse::id, Function.identity()));
        return PageResponse.of(pagina, h -> new HistorialDocumentoResponse(h.getId(), h.getAccion(),
                h.getEstadoAnterior(), h.getEstadoNuevo(), h.getComentario(), porId.get(h.getCreatedBy()),
                h.getCreatedAt()));
    }

    @Transactional
    public DocumentoResponse crear(DocumentoRequest solicitud) {
        validarFechas(solicitud);
        CategoriaDocumento categoria = buscarCategoria(solicitud.categoriaId());
        Empleado responsable = buscarResponsable(solicitud.responsableId());
        // El consecutivo va al final: mantiene bloqueada la secuencia el menor tiempo posible.
        Documento documento = documentos.save(new Documento(consecutivos.siguiente(), solicitud.titulo(),
                solicitud.descripcion(), categoria, responsable, solicitud.fechaDocumento(),
                solicitud.fechaVencimiento()));
        historial.save(new DocumentoHistorial(documento.getId(), AccionHistorial.CREADO, null,
                EstadoDocumento.REGISTRADO, null));
        auditoria.registrar(ENTIDAD, documento.getId(), AccionAuditoria.CREAR, null, foto(documento));
        return aRespuesta(documento);
    }

    @Transactional
    public DocumentoResponse actualizar(Long id, DocumentoRequest solicitud) {
        Documento documento = buscar(id);
        if (solicitud.version() == null) {
            throw ApiException.campo("version", "La versión es obligatoria");
        }
        exigirVersion(documento, solicitud.version());
        exigirEditable(documento);
        validarFechas(solicitud);

        CategoriaDocumento categoria = documento.getCategoria().getId().equals(solicitud.categoriaId())
                ? documento.getCategoria() : buscarCategoria(solicitud.categoriaId());
        Empleado responsable = documento.getResponsable().getId().equals(solicitud.responsableId())
                ? documento.getResponsable() : buscarResponsable(solicitud.responsableId());

        Map<String, Object> anterior = foto(documento);
        documento.actualizar(solicitud.titulo(), solicitud.descripcion(), categoria, responsable,
                solicitud.fechaDocumento(), solicitud.fechaVencimiento());
        historial.save(new DocumentoHistorial(id, AccionHistorial.EDITADO, null, null, null));
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.ACTUALIZAR, anterior, foto(documento));
        return aRespuesta(documento);
    }

    @Transactional
    public DocumentoResponse transitar(Long id, Accion accion, TransicionRequest solicitud) {
        Documento documento = buscar(id);
        exigirVersion(documento, solicitud.version());
        if (!documento.isActivo()) {
            throw ApiException.conflicto("El documento está desactivado");
        }
        EstadoDocumento origen = documento.getEstado();
        if (!accion.aplicaDesde(origen)) {
            throw ApiException.conflicto("Esta acción no está disponible en el estado actual del documento");
        }
        String comentario = solicitud.comentario() == null || solicitud.comentario().isBlank()
                ? null : solicitud.comentario().trim();

        if (accion == Accion.ENVIAR && adjuntos.countByDocumentoIdAndActivoTrue(id) == 0) {
            throw ApiException.conflicto("Agrega al menos un adjunto antes de enviar el documento");
        }
        if (accion == Accion.APROBAR && UsuarioActual.id().equals(documento.getCreatedBy())) {
            throw ApiException.de(TipoError.SEPARACION_FUNCIONES);
        }
        if (accion == Accion.DEVOLVER && comentario == null) {
            throw ApiException.campo("comentario", "Indica por qué se devuelve el documento");
        }

        documento.cambiarEstado(accion.destino());
        historial.save(new DocumentoHistorial(id, AccionHistorial.CAMBIO_ESTADO, origen, accion.destino(), comentario));
        Map<String, Object> anterior = new LinkedHashMap<>();
        anterior.put("estado", origen.name());
        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("estado", accion.destino().name());
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.CAMBIO_ESTADO, anterior, nuevo);
        return aRespuesta(documento);
    }

    @Transactional
    public DocumentoResponse activar(Long id) {
        return cambiarActivo(id, true);
    }

    @Transactional
    public DocumentoResponse desactivar(Long id) {
        return cambiarActivo(id, false);
    }

    private DocumentoResponse cambiarActivo(Long id, boolean activo) {
        Documento documento = buscar(id);
        Map<String, Object> anterior = foto(documento);
        if (activo) {
            documento.activar();
        } else {
            documento.desactivar();
        }
        historial.save(new DocumentoHistorial(id, activo ? AccionHistorial.REACTIVADO : AccionHistorial.DESACTIVADO,
                null, null, null));
        auditoria.registrar(ENTIDAD, id, activo ? AccionAuditoria.ACTIVAR : AccionAuditoria.DESACTIVAR, anterior,
                foto(documento));
        return aRespuesta(documento);
    }

    DocumentoListItemResponse aItem(Documento d) {
        return new DocumentoListItemResponse(d.getId(), d.getConsecutivo(), d.getTitulo(),
                new RefResponse(d.getCategoria().getId(), d.getCategoria().getNombre()),
                new RefResponse(d.getResponsable().getId(), d.getResponsable().nombreCompleto()),
                d.getEstado(), d.getFechaDocumento(), d.getFechaVencimiento(), d.estaVencido(Zona.hoy(reloj)),
                d.isActivo());
    }

    private DocumentoResponse aRespuesta(Documento d) {
        List<AdjuntoResponse> lista = adjuntos.findByDocumentoIdAndActivoTrueOrderById(d.getId()).stream()
                .map(DocumentoService::aAdjunto).toList();
        boolean esCreador = UsuarioActual.id().equals(d.getCreatedBy());
        return new DocumentoResponse(d.getId(), d.getConsecutivo(), d.getTitulo(), d.getDescripcion(),
                new RefResponse(d.getCategoria().getId(), d.getCategoria().getNombre()),
                new RefResponse(d.getResponsable().getId(), d.getResponsable().nombreCompleto()),
                d.getEstado(), d.getFechaDocumento(), d.getFechaVencimiento(), d.estaVencido(Zona.hoy(reloj)),
                d.isActivo(), lista,
                FlujoDocumento.accionesDisponibles(d.getEstado(), d.isActivo(), esCreador, UsuarioActual::tiene),
                d.getVersion(), d.getCreatedAt(), d.getUpdatedAt());
    }

    static AdjuntoResponse aAdjunto(DocumentoAdjunto a) {
        return new AdjuntoResponse(a.getId(), a.getNombreOriginal(), a.getTipoMime(), a.getTamanoBytes(),
                a.getCreatedAt());
    }

    private void exigirVersion(Documento documento, long version) {
        if (documento.getVersion() != version) {
            throw ApiException.de(TipoError.VERSION_DESACTUALIZADA);
        }
    }

    private void exigirEditable(Documento documento) {
        if (!documento.isActivo() || !documento.getEstado().esEditable()) {
            throw ApiException.conflicto("El documento ya no se puede editar en su estado actual");
        }
    }

    private void validarFechas(DocumentoRequest solicitud) {
        if (solicitud.fechaVencimiento() != null && solicitud.fechaVencimiento().isBefore(solicitud.fechaDocumento())) {
            throw ApiException.campo("fechaVencimiento", "No puede ser anterior a la fecha del documento");
        }
    }

    private Documento buscar(Long id) {
        return documentos.buscarCompleto(id).orElseThrow(() -> ApiException.noEncontrado("El documento no existe"));
    }

    private CategoriaDocumento buscarCategoria(Long id) {
        return categorias.findById(id).filter(CategoriaDocumento::isActivo)
                .orElseThrow(() -> ApiException.campo("categoriaId", "La categoría no existe o está inactiva"));
    }

    private Empleado buscarResponsable(Long id) {
        return empleados.findById(id).filter(Empleado::isActivo)
                .orElseThrow(() -> ApiException.campo("responsableId", "El responsable no existe o está inactivo"));
    }

    private Map<String, Object> foto(Documento d) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("consecutivo", d.getConsecutivo());
        campos.put("titulo", d.getTitulo());
        campos.put("categoria", d.getCategoria().getNombre());
        campos.put("responsable", d.getResponsable().nombreCompleto());
        campos.put("estado", d.getEstado().name());
        campos.put("fechaDocumento", d.getFechaDocumento().toString());
        campos.put("fechaVencimiento", d.getFechaVencimiento() == null ? null : d.getFechaVencimiento().toString());
        campos.put("activo", d.isActivo());
        return campos;
    }
}
