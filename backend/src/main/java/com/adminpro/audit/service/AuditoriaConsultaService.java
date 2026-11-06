package com.adminpro.audit.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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

import com.adminpro.audit.dto.AuditoriaResponse;
import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.entity.Auditoria;
import com.adminpro.audit.repository.AuditoriaRepository;
import com.adminpro.common.Consultas;
import com.adminpro.common.PageResponse;
import com.adminpro.common.RefResponse;
import com.adminpro.exception.ApiException;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class AuditoriaConsultaService {

    private static final Set<String> ORDEN_PERMITIDO = Set.of("createdAt", "entidad", "accion");

    private final AuditoriaRepository repositorio;
    private final UsuarioRepository usuarios;

    public AuditoriaConsultaService(AuditoriaRepository repositorio, UsuarioRepository usuarios) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
    }

    public PageResponse<AuditoriaResponse> listar(String entidad, Long entidadId, Long usuarioId,
            AccionAuditoria accion, OffsetDateTime desde, OffsetDateTime hasta, Pageable paginacion) {
        Consultas.validarOrden(paginacion, ORDEN_PERMITIDO);

        List<Specification<Auditoria>> filtros = new ArrayList<>();
        if (entidad != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("entidad"), entidad));
        }
        if (entidadId != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("entidadId"), entidadId));
        }
        if (usuarioId != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("createdBy"), usuarioId));
        }
        if (accion != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("accion"), accion));
        }
        if (desde != null) {
            filtros.add((raiz, consulta, cb) ->
                    cb.greaterThanOrEqualTo(raiz.get("createdAt"), desde.toInstant()));
        }
        if (hasta != null) {
            filtros.add((raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get("createdAt"), hasta.toInstant()));
        }

        Page<Auditoria> pagina = repositorio.findAll(Specification.allOf(filtros), paginacion);
        Map<Long, RefResponse> autores = autoresDe(pagina.getContent());
        return PageResponse.of(pagina, a -> aRespuesta(a, autores));
    }

    /** Historial de un registro para quien lo puede leer, sin datos técnicos como la IP. */
    public PageResponse<AuditoriaResponse> historialDe(String entidad, Long entidadId, Pageable paginacion) {
        Consultas.validarOrden(paginacion, ORDEN_PERMITIDO);
        Specification<Auditoria> filtro = (raiz, consulta, cb) -> cb.and(
                cb.equal(raiz.get("entidad"), entidad), cb.equal(raiz.get("entidadId"), entidadId));
        Page<Auditoria> pagina = repositorio.findAll(filtro, paginacion);
        Map<Long, RefResponse> autores = autoresDe(pagina.getContent());
        return PageResponse.of(pagina, a -> new AuditoriaResponse(a.getId(), a.getEntidad(), a.getEntidadId(),
                a.getAccion(), a.getValorAnterior(), a.getValorNuevo(), autores.get(a.getCreatedBy()), null,
                a.getCreatedAt()));
    }

    public AuditoriaResponse obtener(Long id) {
        Auditoria auditoria = repositorio.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("El registro de auditoría no existe"));
        return aRespuesta(auditoria, autoresDe(List.of(auditoria)));
    }

    /** Resuelve los autores de la página en una sola consulta, no uno por fila. */
    private Map<Long, RefResponse> autoresDe(List<Auditoria> registros) {
        Set<Long> ids = registros.stream()
                .map(Auditoria::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return usuarios.findAllById(ids).stream()
                .map(u -> new RefResponse(u.getId(), u.getNombreVisible()))
                .collect(Collectors.toMap(RefResponse::id, Function.identity()));
    }

    private AuditoriaResponse aRespuesta(Auditoria a, Map<Long, RefResponse> autores) {
        return new AuditoriaResponse(a.getId(), a.getEntidad(), a.getEntidadId(), a.getAccion(),
                a.getValorAnterior(), a.getValorNuevo(), autores.get(a.getCreatedBy()), a.getIp(), a.getCreatedAt());
    }
}
