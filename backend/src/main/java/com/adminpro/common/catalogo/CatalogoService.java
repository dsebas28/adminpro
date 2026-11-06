package com.adminpro.common.catalogo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.common.Consultas;
import com.adminpro.common.PageResponse;
import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;

import jakarta.persistence.criteria.Expression;

/** Reglas comunes de los catálogos. Cada uno define cómo se crea y cuándo está en uso. */
@Transactional(readOnly = true)
public abstract class CatalogoService<E extends CatalogoEntity> {

    private static final Set<String> ORDEN_PERMITIDO = Set.of("nombre", "activo");

    private final CatalogoRepository<E> repositorio;
    private final AuditService auditoria;
    private final String entidad;
    private final String etiqueta;

    protected CatalogoService(CatalogoRepository<E> repositorio, AuditService auditoria, String entidad,
            String etiqueta) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.entidad = entidad;
        this.etiqueta = etiqueta;
    }

    protected abstract E nueva(String nombre, String descripcion);

    /** Un catálogo con registros activos no se puede desactivar (supuesto S14). */
    protected abstract boolean enUso(Long id);

    public PageResponse<CatalogoResponse> listar(String q, Boolean activo, Pageable paginacion) {
        Consultas.validarOrden(paginacion, ORDEN_PERMITIDO);
        List<Specification<E>> filtros = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            filtros.add((raiz, consulta, cb) -> {
                Expression<String> nombre = cb.function("f_normalizar", String.class, raiz.get("nombre"));
                Expression<String> buscado = cb.function("f_normalizar", String.class,
                        cb.literal(Consultas.patronContiene(q)));
                return cb.like(nombre, buscado, '\\');
            });
        }
        if (activo != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("activo"), activo));
        }
        return PageResponse.of(repositorio.findAll(Specification.allOf(filtros), paginacion), CatalogoResponse::de);
    }

    public CatalogoResponse obtener(Long id) {
        return CatalogoResponse.de(buscar(id));
    }

    @Transactional
    public CatalogoResponse crear(CatalogoRequest solicitud) {
        if (repositorio.existsByNombreIgnoreCase(solicitud.nombre())) {
            throw ApiException.conflicto("Ya existe un " + etiqueta + " con ese nombre");
        }
        E creado = repositorio.save(nueva(solicitud.nombre(), solicitud.descripcion()));
        auditoria.registrar(entidad, creado.getId(), AccionAuditoria.CREAR, null, foto(creado));
        return CatalogoResponse.de(creado);
    }

    @Transactional
    public CatalogoResponse actualizar(Long id, CatalogoRequest solicitud) {
        E registro = buscar(id);
        if (solicitud.version() == null) {
            throw ApiException.campo("version", "La versión es obligatoria");
        }
        if (registro.getVersion() != solicitud.version()) {
            throw ApiException.de(TipoError.VERSION_DESACTUALIZADA);
        }
        if (repositorio.existsByNombreIgnoreCaseAndIdNot(solicitud.nombre(), id)) {
            throw ApiException.conflicto("Ya existe un " + etiqueta + " con ese nombre");
        }
        Map<String, Object> anterior = foto(registro);
        registro.actualizar(solicitud.nombre(), solicitud.descripcion());
        auditoria.registrar(entidad, id, AccionAuditoria.ACTUALIZAR, anterior, foto(registro));
        return CatalogoResponse.de(registro);
    }

    @Transactional
    public CatalogoResponse activar(Long id) {
        E registro = buscar(id);
        Map<String, Object> anterior = foto(registro);
        registro.activar();
        auditoria.registrar(entidad, id, AccionAuditoria.ACTIVAR, anterior, foto(registro));
        return CatalogoResponse.de(registro);
    }

    @Transactional
    public CatalogoResponse desactivar(Long id) {
        E registro = buscar(id);
        if (enUso(id)) {
            throw ApiException.conflicto("No se puede desactivar: tiene registros activos asociados");
        }
        Map<String, Object> anterior = foto(registro);
        registro.desactivar();
        auditoria.registrar(entidad, id, AccionAuditoria.DESACTIVAR, anterior, foto(registro));
        return CatalogoResponse.de(registro);
    }

    private E buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("El " + etiqueta + " no existe"));
    }

    private Map<String, Object> foto(E registro) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("nombre", registro.getNombre());
        campos.put("descripcion", registro.getDescripcion());
        campos.put("activo", registro.isActivo());
        return campos;
    }
}
