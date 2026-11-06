package com.adminpro.empleados.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.dto.AuditoriaResponse;
import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.audit.service.AuditoriaConsultaService;
import com.adminpro.common.Consultas;
import com.adminpro.common.PageResponse;
import com.adminpro.common.Zona;
import com.adminpro.empleados.dto.EmpleadoRequest;
import com.adminpro.empleados.dto.EmpleadoResponse;
import com.adminpro.empleados.entity.Cargo;
import com.adminpro.empleados.entity.Departamento;
import com.adminpro.empleados.entity.Empleado;
import com.adminpro.empleados.mapper.EmpleadoMapper;
import com.adminpro.empleados.repository.CargoRepository;
import com.adminpro.empleados.repository.DepartamentoRepository;
import com.adminpro.empleados.repository.EmpleadoRepository;
import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;
import com.adminpro.usuarios.service.UsuarioService;

import jakarta.persistence.criteria.Expression;

@Service
@Transactional(readOnly = true)
public class EmpleadoService {

    private static final String ENTIDAD = "EMPLEADO";
    private static final Set<String> ORDEN_PERMITIDO =
            Set.of("apellidos", "nombres", "cedula", "correo", "fechaIngreso", "activo");

    private final EmpleadoRepository empleados;
    private final DepartamentoRepository departamentos;
    private final CargoRepository cargos;
    private final UsuarioService usuarios;
    private final AuditService auditoria;
    private final AuditoriaConsultaService consultaAuditoria;
    private final EmpleadoMapper mapper;
    private final Clock reloj;

    public EmpleadoService(EmpleadoRepository empleados, DepartamentoRepository departamentos,
            CargoRepository cargos, UsuarioService usuarios, AuditService auditoria,
            AuditoriaConsultaService consultaAuditoria, EmpleadoMapper mapper, Clock reloj) {
        this.empleados = empleados;
        this.departamentos = departamentos;
        this.cargos = cargos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.consultaAuditoria = consultaAuditoria;
        this.mapper = mapper;
        this.reloj = reloj;
    }

    public PageResponse<EmpleadoResponse> listar(String q, Long departamentoId, Long cargoId, Boolean activo,
            Pageable paginacion) {
        Consultas.validarOrden(paginacion, ORDEN_PERMITIDO);

        List<Specification<Empleado>> filtros = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            // Repite la expresión del índice ix_empleados_busqueda para poder usarlo.
            filtros.add((raiz, consulta, cb) -> {
                Expression<String> texto = cb.concat(cb.concat(cb.concat(cb.concat(cb.concat(cb.concat(
                        raiz.<String>get("nombres"), " "), raiz.<String>get("apellidos")), " "),
                        raiz.<String>get("cedula")), " "), raiz.<String>get("correo"));
                return cb.like(cb.function("f_normalizar", String.class, texto),
                        cb.function("f_normalizar", String.class, cb.literal(Consultas.patronContiene(q))), '\\');
            });
        }
        if (departamentoId != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("departamento").get("id"), departamentoId));
        }
        if (cargoId != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("cargo").get("id"), cargoId));
        }
        if (activo != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("activo"), activo));
        }
        return PageResponse.of(empleados.findAll(Specification.allOf(filtros), paginacion), mapper::aRespuesta);
    }

    public EmpleadoResponse obtener(Long id) {
        return mapper.aRespuesta(buscar(id));
    }

    public PageResponse<AuditoriaResponse> historial(Long id, Pageable paginacion) {
        buscar(id);
        return consultaAuditoria.historialDe(ENTIDAD, id, paginacion);
    }

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest solicitud) {
        validarFecha(solicitud.fechaIngreso());
        if (empleados.existsByCedula(solicitud.cedula())) {
            throw ApiException.conflictoEn("cedula", "Ya existe un empleado con esa cédula");
        }
        if (empleados.existsByCorreoIgnoreCase(solicitud.correo())) {
            throw ApiException.conflictoEn("correo", "Ya existe un empleado con ese correo");
        }
        Empleado empleado = empleados.save(new Empleado(solicitud.cedula(), solicitud.nombres(),
                solicitud.apellidos(), solicitud.correo(), solicitud.telefono(), solicitud.fechaIngreso(),
                buscarDepartamento(solicitud.departamentoId()), buscarCargo(solicitud.cargoId())));
        auditoria.registrar(ENTIDAD, empleado.getId(), AccionAuditoria.CREAR, null, foto(empleado));
        return mapper.aRespuesta(empleado);
    }

    @Transactional
    public EmpleadoResponse actualizar(Long id, EmpleadoRequest solicitud) {
        Empleado empleado = buscar(id);
        if (solicitud.version() == null) {
            throw ApiException.campo("version", "La versión es obligatoria");
        }
        if (empleado.getVersion() != solicitud.version()) {
            throw ApiException.de(TipoError.VERSION_DESACTUALIZADA);
        }
        validarFecha(solicitud.fechaIngreso());
        if (empleados.existsByCedulaAndIdNot(solicitud.cedula(), id)) {
            throw ApiException.conflictoEn("cedula", "Ya existe un empleado con esa cédula");
        }
        if (empleados.existsByCorreoIgnoreCaseAndIdNot(solicitud.correo(), id)) {
            throw ApiException.conflictoEn("correo", "Ya existe un empleado con ese correo");
        }
        Map<String, Object> anterior = foto(empleado);
        empleado.cambiarCedula(solicitud.cedula());
        empleado.actualizar(solicitud.nombres(), solicitud.apellidos(), solicitud.correo(), solicitud.telefono(),
                solicitud.fechaIngreso(), departamentoPara(empleado, solicitud.departamentoId()),
                cargoPara(empleado, solicitud.cargoId()));
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.ACTUALIZAR, anterior, foto(empleado));
        return mapper.aRespuesta(empleado);
    }

    @Transactional
    public EmpleadoResponse activar(Long id) {
        Empleado empleado = buscar(id);
        Map<String, Object> anterior = foto(empleado);
        empleado.activar();
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.ACTIVAR, anterior, foto(empleado));
        return mapper.aRespuesta(empleado);
    }

    /** También desactiva su usuario, si tiene uno (supuesto S13). */
    @Transactional
    public EmpleadoResponse desactivar(Long id) {
        Empleado empleado = buscar(id);
        usuarios.desactivarDeEmpleado(id);
        Map<String, Object> anterior = foto(empleado);
        empleado.desactivar();
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.DESACTIVAR, anterior, foto(empleado));
        return mapper.aRespuesta(empleado);
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(Zona.hoy(reloj))) {
            throw ApiException.campo("fechaIngreso", "La fecha de ingreso no puede ser futura");
        }
    }

    private Empleado buscar(Long id) {
        return empleados.buscarCompleto(id).orElseThrow(() -> ApiException.noEncontrado("El empleado no existe"));
    }

    /** Conservar un departamento ya desactivado es válido; asignar uno nuevo inactivo, no. */
    private Departamento departamentoPara(Empleado empleado, Long id) {
        return empleado.getDepartamento().getId().equals(id) ? empleado.getDepartamento() : buscarDepartamento(id);
    }

    private Cargo cargoPara(Empleado empleado, Long id) {
        return empleado.getCargo().getId().equals(id) ? empleado.getCargo() : buscarCargo(id);
    }

    private Departamento buscarDepartamento(Long id) {
        return departamentos.findById(id).filter(Departamento::isActivo)
                .orElseThrow(() -> ApiException.campo("departamentoId", "El departamento no existe o está inactivo"));
    }

    private Cargo buscarCargo(Long id) {
        return cargos.findById(id).filter(Cargo::isActivo)
                .orElseThrow(() -> ApiException.campo("cargoId", "El cargo no existe o está inactivo"));
    }

    private Map<String, Object> foto(Empleado empleado) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("cedula", empleado.getCedula());
        campos.put("nombres", empleado.getNombres());
        campos.put("apellidos", empleado.getApellidos());
        campos.put("correo", empleado.getCorreo());
        campos.put("telefono", empleado.getTelefono());
        campos.put("fechaIngreso", empleado.getFechaIngreso().toString());
        campos.put("departamento", empleado.getDepartamento().getNombre());
        campos.put("cargo", empleado.getCargo().getNombre());
        campos.put("activo", empleado.isActivo());
        return campos;
    }
}
