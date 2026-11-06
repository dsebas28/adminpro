package com.adminpro.usuarios.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.auth.service.RefreshTokenService;
import com.adminpro.common.Consultas;
import com.adminpro.common.PageResponse;
import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;
import com.adminpro.security.PasswordPolicy;
import com.adminpro.security.UsuarioActual;
import com.adminpro.usuarios.dto.RestablecerPasswordRequest;
import com.adminpro.usuarios.dto.RolResponse;
import com.adminpro.usuarios.dto.UsuarioCreateRequest;
import com.adminpro.usuarios.dto.UsuarioResponse;
import com.adminpro.usuarios.dto.UsuarioUpdateRequest;
import com.adminpro.usuarios.entity.EmpleadoRef;
import com.adminpro.usuarios.entity.Rol;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.mapper.UsuarioMapper;
import com.adminpro.usuarios.repository.EmpleadoRefRepository;
import com.adminpro.usuarios.repository.RolRepository;
import com.adminpro.usuarios.repository.UsuarioRepository;

import jakarta.persistence.criteria.Expression;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final String ENTIDAD = "USUARIO";
    private static final Set<String> ORDEN_PERMITIDO = Set.of("correo", "nombreVisible", "activo", "ultimoAccesoAt");

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final EmpleadoRefRepository empleados;
    private final PasswordEncoder codificador;
    private final RefreshTokenService refreshTokens;
    private final AuditService auditoria;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, EmpleadoRefRepository empleados,
            PasswordEncoder codificador, RefreshTokenService refreshTokens, AuditService auditoria,
            UsuarioMapper mapper) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.empleados = empleados;
        this.codificador = codificador;
        this.refreshTokens = refreshTokens;
        this.auditoria = auditoria;
        this.mapper = mapper;
    }

    public PageResponse<UsuarioResponse> listar(String q, Long rolId, Boolean activo, Pageable paginacion) {
        Consultas.validarOrden(paginacion, ORDEN_PERMITIDO);

        List<Specification<Usuario>> filtros = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            filtros.add((raiz, consulta, cb) -> {
                Expression<String> texto = cb.concat(cb.concat(raiz.get("correo"), " "), raiz.get("nombreVisible"));
                Expression<String> normalizado = cb.function("f_normalizar", String.class, texto);
                Expression<String> buscado = cb.function("f_normalizar", String.class,
                        cb.literal(Consultas.patronContiene(q)));
                return cb.like(normalizado, buscado, '\\');
            });
        }
        if (rolId != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("rol").get("id"), rolId));
        }
        if (activo != null) {
            filtros.add((raiz, consulta, cb) -> cb.equal(raiz.get("activo"), activo));
        }
        return PageResponse.of(usuarios.findAll(Specification.allOf(filtros), paginacion), mapper::aRespuesta);
    }

    public UsuarioResponse obtener(Long id) {
        return mapper.aRespuesta(buscar(id));
    }

    public List<RolResponse> listarRoles() {
        return roles.listarConPermisos().stream().map(mapper::aRespuesta).toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest solicitud) {
        PasswordPolicy.validar("passwordTemporal", solicitud.passwordTemporal());
        if (usuarios.existeCorreo(solicitud.correo())) {
            throw ApiException.conflicto("Ya existe un usuario con ese correo");
        }
        EmpleadoRef empleado = buscarEmpleado(solicitud.empleadoId());
        if (empleado != null && usuarios.existsByEmpleado_Id(empleado.getId())) {
            throw ApiException.conflicto("Ese empleado ya tiene un usuario");
        }
        Usuario usuario = usuarios.save(new Usuario(solicitud.correo(),
                codificador.encode(solicitud.passwordTemporal()), solicitud.nombreVisible(),
                buscarRol(solicitud.rolId()), empleado));
        auditoria.registrar(ENTIDAD, usuario.getId(), AccionAuditoria.CREAR, null, foto(usuario));
        return mapper.aRespuesta(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest solicitud) {
        Usuario usuario = buscar(id);
        if (usuario.getVersion() != solicitud.version()) {
            throw ApiException.de(TipoError.VERSION_DESACTUALIZADA);
        }
        Rol rol = buscarRol(solicitud.rolId());
        EmpleadoRef empleado = buscarEmpleado(solicitud.empleadoId());
        if (empleado != null && usuarios.existsByEmpleado_IdAndIdNot(empleado.getId(), id)) {
            throw ApiException.conflicto("Ese empleado ya tiene un usuario");
        }
        if (!rol.getCodigo().equals(Rol.ADMINISTRADOR)) {
            exigirOtroAdministrador(usuario);
        }
        Map<String, Object> anterior = foto(usuario);
        usuario.actualizar(solicitud.nombreVisible(), rol, empleado);
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.ACTUALIZAR, anterior, foto(usuario));
        return mapper.aRespuesta(usuario);
    }

    @Transactional
    public void restablecerPassword(Long id, RestablecerPasswordRequest solicitud) {
        PasswordPolicy.validar("passwordTemporal", solicitud.passwordTemporal());
        Usuario usuario = buscar(id);
        usuario.asignarPasswordTemporal(codificador.encode(solicitud.passwordTemporal()));
        refreshTokens.revocarTodas(id);
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.RESTABLECER_PASSWORD, null, null);
    }

    @Transactional
    public UsuarioResponse activar(Long id) {
        Usuario usuario = buscar(id);
        Map<String, Object> anterior = foto(usuario);
        usuario.activar();
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.ACTIVAR, anterior, foto(usuario));
        return mapper.aRespuesta(usuario);
    }

    @Transactional
    public UsuarioResponse desactivar(Long id) {
        Usuario usuario = buscar(id);
        if (id.equals(UsuarioActual.id())) {
            throw ApiException.conflicto("No puedes desactivar tu propio usuario");
        }
        exigirOtroAdministrador(usuario);
        Map<String, Object> anterior = foto(usuario);
        usuario.desactivar();
        refreshTokens.revocarTodas(id);
        auditoria.registrar(ENTIDAD, id, AccionAuditoria.DESACTIVAR, anterior, foto(usuario));
        return mapper.aRespuesta(usuario);
    }

    /** Un empleado que se va pierde el acceso. No aplica sobre uno mismo ni sobre el último Administrador. */
    @Transactional
    public void desactivarDeEmpleado(Long empleadoId) {
        usuarios.findByEmpleado_Id(empleadoId).filter(Usuario::isActivo).ifPresent(usuario -> {
            if (usuario.getId().equals(UsuarioActual.id())) {
                throw ApiException.conflicto("No puedes desactivar al empleado vinculado a tu propio usuario");
            }
            exigirOtroAdministrador(usuario);
            Map<String, Object> anterior = foto(usuario);
            usuario.desactivar();
            refreshTokens.revocarTodas(usuario.getId());
            auditoria.registrar(ENTIDAD, usuario.getId(), AccionAuditoria.DESACTIVAR, anterior, foto(usuario));
        });
    }

    /** Sin un Administrador activo nadie podría gestionar usuarios desde la aplicación. */
    private void exigirOtroAdministrador(Usuario usuario) {
        boolean esAdministradorActivo = usuario.isActivo() && usuario.getRol().getCodigo().equals(Rol.ADMINISTRADOR);
        if (esAdministradorActivo && usuarios.countByRol_CodigoAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw ApiException.conflicto("Debe quedar al menos un Administrador activo");
        }
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarCompleto(id)
                .orElseThrow(() -> ApiException.noEncontrado("El usuario no existe"));
    }

    private Rol buscarRol(Long id) {
        return roles.findById(id).orElseThrow(() -> ApiException.campo("rolId", "El rol no existe"));
    }

    private EmpleadoRef buscarEmpleado(Long id) {
        if (id == null) {
            return null;
        }
        EmpleadoRef empleado = empleados.findById(id)
                .orElseThrow(() -> ApiException.campo("empleadoId", "El empleado no existe"));
        if (!empleado.isActivo()) {
            throw ApiException.campo("empleadoId", "El empleado está inactivo");
        }
        return empleado;
    }

    /** Campos que van a la auditoría. La lista es explícita para que nunca incluya el hash. */
    private Map<String, Object> foto(Usuario usuario) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("correo", usuario.getCorreo());
        campos.put("nombreVisible", usuario.getNombreVisible());
        campos.put("rol", usuario.getRol().getCodigo());
        campos.put("empleadoId", usuario.getEmpleado() == null ? null : usuario.getEmpleado().getId());
        campos.put("activo", usuario.isActivo());
        return campos;
    }
}
