package com.adminpro.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.auth.service.RefreshTokenService;
import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;
import com.adminpro.usuarios.dto.RestablecerPasswordRequest;
import com.adminpro.usuarios.dto.UsuarioCreateRequest;
import com.adminpro.usuarios.dto.UsuarioUpdateRequest;
import com.adminpro.usuarios.entity.EmpleadoRef;
import com.adminpro.usuarios.entity.Rol;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.mapper.UsuarioMapperImpl;
import com.adminpro.usuarios.repository.EmpleadoRefRepository;
import com.adminpro.usuarios.repository.RolRepository;
import com.adminpro.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Long ACTOR = 1L;
    private static final Long OBJETIVO = 2L;

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private RolRepository roles;
    @Mock
    private EmpleadoRefRepository empleados;
    @Mock
    private PasswordEncoder codificador;
    @Mock
    private RefreshTokenService refreshTokens;
    @Mock
    private AuditService auditoria;

    private UsuarioService servicio;
    private Rol administrador;
    private Rol auxiliar;

    @BeforeEach
    void configurar() {
        servicio = new UsuarioService(usuarios, roles, empleados, codificador, refreshTokens, auditoria,
                new UsuarioMapperImpl());
        administrador = rol(Rol.ADMINISTRADOR, 10L);
        auxiliar = rol("AUXILIAR", 20L);

        Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), java.util.Map.of("alg", "none"),
                java.util.Map.of("sub", ACTOR.toString()));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministrador() {
        Usuario objetivo = usuario(OBJETIVO, administrador);
        when(usuarios.buscarCompleto(OBJETIVO)).thenReturn(Optional.of(objetivo));
        when(usuarios.countByRol_CodigoAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.desactivar(OBJETIVO))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("al menos un Administrador");
        assertThat(objetivo.isActivo()).isTrue();
    }

    @Test
    void conOtroAdministradorActivoSiSePuedeDesactivar() {
        Usuario objetivo = usuario(OBJETIVO, administrador);
        when(usuarios.buscarCompleto(OBJETIVO)).thenReturn(Optional.of(objetivo));
        when(usuarios.countByRol_CodigoAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(2L);

        servicio.desactivar(OBJETIVO);

        assertThat(objetivo.isActivo()).isFalse();
        verify(refreshTokens).revocarTodas(OBJETIVO);
    }

    @Test
    void noSePuedeDesactivarElPropioUsuario() {
        when(usuarios.buscarCompleto(ACTOR)).thenReturn(Optional.of(usuario(ACTOR, auxiliar)));

        assertThatThrownBy(() -> servicio.desactivar(ACTOR))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("tu propio usuario");
    }

    @Test
    void cambiarElRolDelUltimoAdministradorEsConflicto() {
        when(usuarios.buscarCompleto(OBJETIVO)).thenReturn(Optional.of(usuario(OBJETIVO, administrador)));
        when(roles.findById(20L)).thenReturn(Optional.of(auxiliar));
        when(usuarios.countByRol_CodigoAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.actualizar(OBJETIVO, new UsuarioUpdateRequest("N", 20L, null, 0L)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("al menos un Administrador");
    }

    @Test
    void versionDistintaEsVersionDesactualizada() {
        when(usuarios.buscarCompleto(OBJETIVO)).thenReturn(Optional.of(usuario(OBJETIVO, auxiliar)));

        assertThatThrownBy(() -> servicio.actualizar(OBJETIVO, new UsuarioUpdateRequest("N", 20L, null, 5L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.tipo()).isEqualTo(TipoError.VERSION_DESACTUALIZADA));
    }

    @Test
    void crearConCorreoExistenteEsConflicto() {
        when(usuarios.existeCorreo("a@b.co")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(solicitud(null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("correo");
        verify(usuarios, never()).save(any());
    }

    @Test
    void empleadoInactivoNoSePuedeVincular() {
        EmpleadoRef empleado = mock(EmpleadoRef.class);
        when(empleado.isActivo()).thenReturn(false);
        when(empleados.findById(5L)).thenReturn(Optional.of(empleado));

        assertThatThrownBy(() -> servicio.crear(solicitud(5L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.errores().getFirst().field()).isEqualTo("empleadoId"));
    }

    @Test
    void passwordTemporalDeMasDe72BytesSeRechazaConCampo() {
        var solicitud = new UsuarioCreateRequest("a@b.co", "N", 20L, null, "ñ".repeat(60));

        assertThatThrownBy(() -> servicio.crear(solicitud))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.errores().getFirst().field()).isEqualTo("passwordTemporal"));
    }

    @Test
    void restablecerDesbloqueaRevocaSesionesYObligaACambiar() {
        Usuario objetivo = usuario(OBJETIVO, auxiliar);
        objetivo.registrarFallo(1, Instant.now().plusSeconds(900));
        when(usuarios.buscarCompleto(OBJETIVO)).thenReturn(Optional.of(objetivo));
        when(codificador.encode("Temporal.2026")).thenReturn("hash");

        servicio.restablecerPassword(OBJETIVO, new RestablecerPasswordRequest("Temporal.2026"));

        assertThat(objetivo.getBloqueadoHasta()).isNull();
        assertThat(objetivo.isDebeCambiarPassword()).isTrue();
        verify(refreshTokens).revocarTodas(OBJETIVO);
        verify(auditoria).registrar("USUARIO", OBJETIVO, AccionAuditoria.RESTABLECER_PASSWORD, null, null);
    }

    private UsuarioCreateRequest solicitud(Long empleadoId) {
        return new UsuarioCreateRequest("a@b.co", "N", 20L, empleadoId, "Temporal.2026");
    }

    private Rol rol(String codigo, Long id) {
        Rol rol = mock(Rol.class);
        lenient().when(rol.getCodigo()).thenReturn(codigo);
        lenient().when(rol.getId()).thenReturn(id);
        return rol;
    }

    private Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario("u@adminpro.test", "h", "U", rol, null);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
