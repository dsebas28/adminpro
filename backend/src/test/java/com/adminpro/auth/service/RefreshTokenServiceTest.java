package com.adminpro.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.auth.entity.RefreshToken;
import com.adminpro.auth.repository.RefreshTokenRepository;
import com.adminpro.security.SecurityProperties;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-06T12:00:00Z");
    private static final Long USUARIO = 7L;

    @Mock
    private RefreshTokenRepository tokens;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private AuditService auditoria;

    private RefreshTokenService servicio;
    private final UUID familia = UUID.randomUUID();

    @BeforeEach
    void configurar() {
        SecurityProperties propiedades = new SecurityProperties("x", Duration.ofMinutes(15), Duration.ofDays(7),
                Duration.ofSeconds(10), true, 5, Duration.ofMinutes(15), 10);
        servicio = new RefreshTokenService(tokens, usuarios, auditoria, propiedades,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void tokenDesconocidoNoRota() {
        when(tokens.buscarParaRotar(anyString())).thenReturn(Optional.empty());

        assertThat(servicio.rotar("x", "ip", "ua")).isEmpty();
    }

    @Test
    void tokenVencidoNoRota() {
        conToken(token(AHORA.minusSeconds(1), null));

        assertThat(servicio.rotar("x", "ip", "ua")).isEmpty();
        verify(tokens, never()).save(any());
    }

    @Test
    void tokenValidoSeRevocaYEmiteOtroDeLaMismaFamilia() {
        RefreshToken token = token(AHORA.plusSeconds(60), null);
        conToken(token);
        familiaActiva(true);
        conUsuario(true);

        var rotacion = servicio.rotar("x", "ip", "ua");

        assertThat(rotacion).isPresent();
        assertThat(token.getRevocadoAt()).isEqualTo(AHORA);
        verify(tokens).save(any(RefreshToken.class));
    }

    @Test
    void reutilizacionFueraDeLaGraciaRevocaLaFamiliaYAudita() {
        conToken(token(AHORA.plusSeconds(60), AHORA.minusSeconds(11)));
        familiaActiva(true);

        assertThat(servicio.rotar("x", "ip", "ua")).isEmpty();

        verify(tokens).revocarFamilia(familia, AHORA);
        verify(auditoria).registrarEvento("USUARIO", USUARIO, AccionAuditoria.REUTILIZACION_TOKEN, USUARIO);
    }

    @Test
    void dentroDeLaGraciaSeEmiteUnTokenSinRevocarLaFamilia() {
        conToken(token(AHORA.plusSeconds(60), AHORA.minusSeconds(5)));
        familiaActiva(true);
        conUsuario(true);

        assertThat(servicio.rotar("x", "ip", "ua")).isPresent();

        verify(tokens, never()).revocarFamilia(any(), any());
        verify(auditoria, never()).registrarEvento(any(), any(), any(), any());
    }

    @Test
    void trasCerrarSesionUnTokenRecienteNoSeBeneficiaDeLaGracia() {
        conToken(token(AHORA.plusSeconds(60), AHORA.minusSeconds(1)));
        familiaActiva(false);

        assertThat(servicio.rotar("x", "ip", "ua")).isEmpty();

        verify(tokens, never()).save(any());
        verify(auditoria, never()).registrarEvento(any(), any(), any(), any());
    }

    @Test
    void usuarioInactivoRevocaLaFamilia() {
        conToken(token(AHORA.plusSeconds(60), null));
        familiaActiva(true);
        conUsuario(false);

        assertThat(servicio.rotar("x", "ip", "ua")).isEmpty();

        verify(tokens).revocarFamilia(familia, AHORA);
    }

    @Test
    void sinTokenActualSeRevocanTodasLasSesiones() {
        servicio.revocarOtrasSesiones(USUARIO, null);

        verify(tokens).revocarDeUsuario(USUARIO, AHORA);
    }

    @Test
    void unTokenDeOtroUsuarioNoSirveParaConservarSesion() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(
                new RefreshToken(99L, "h", familia, AHORA.plusSeconds(60), null, null)));

        servicio.revocarOtrasSesiones(USUARIO, "ajeno");

        verify(tokens).revocarDeUsuario(USUARIO, AHORA);
        verify(tokens, never()).revocarOtrasFamilias(any(), any(), any());
    }

    private RefreshToken token(Instant expira, Instant revocado) {
        RefreshToken token = new RefreshToken(USUARIO, "h", familia, expira, null, null);
        if (revocado != null) {
            token.revocar(revocado);
        }
        return token;
    }

    private void conToken(RefreshToken token) {
        when(tokens.buscarParaRotar(anyString())).thenReturn(Optional.of(token));
    }

    private void familiaActiva(boolean activa) {
        when(tokens.existsByFamiliaIdAndRevocadoAtIsNullAndExpiraAtAfter(eq(familia), any())).thenReturn(activa);
    }

    private void conUsuario(boolean activo) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.isActivo()).thenReturn(activo);
        if (activo) {
            when(usuario.getId()).thenReturn(USUARIO);
        }
        when(usuarios.buscarCompleto(USUARIO)).thenReturn(Optional.of(usuario));
    }
}
