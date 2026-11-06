package com.adminpro.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.auth.entity.RefreshToken;
import com.adminpro.auth.repository.RefreshTokenRepository;
import com.adminpro.security.SecurityProperties;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.UsuarioRepository;

/**
 * Rotación de refresh tokens con detección de robo (D-10). Los métodos devuelven un
 * Optional vacío en lugar de lanzar una excepción, porque una excepción revertiría la
 * transacción y con ella la revocación de la familia, que debe quedar guardada.
 */
@Service
public class RefreshTokenService {

    private static final int BYTES_TOKEN = 32;

    public record Rotacion(Usuario usuario, String tokenNuevo) {
    }

    private final RefreshTokenRepository tokens;
    private final UsuarioRepository usuarios;
    private final AuditService auditoria;
    private final SecurityProperties propiedades;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, UsuarioRepository usuarios, AuditService auditoria,
            SecurityProperties propiedades, Clock reloj) {
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public String emitirNuevaSesion(Long usuarioId, String ip, String userAgent) {
        return emitir(usuarioId, UUID.randomUUID(), ip, userAgent);
    }

    @Transactional
    public Optional<Rotacion> rotar(String tokenPlano, String ip, String userAgent) {
        Instant ahora = reloj.instant();
        Optional<RefreshToken> encontrado = tokens.buscarParaRotar(hash(tokenPlano));
        if (encontrado.isEmpty() || encontrado.get().estaVencido(ahora)) {
            return Optional.empty();
        }
        RefreshToken token = encontrado.get();
        boolean familiaActiva = tokens.existsByFamiliaIdAndRevocadoAtIsNullAndExpiraAtAfter(token.getFamiliaId(), ahora);

        if (token.estaRevocado() && !dentroDeGracia(token, ahora, familiaActiva)) {
            if (familiaActiva) { // Sin familia activa solo es una sesión ya cerrada, no un robo.
                tokens.revocarFamilia(token.getFamiliaId(), ahora);
                auditoria.registrarEvento("USUARIO", token.getUsuarioId(), AccionAuditoria.REUTILIZACION_TOKEN,
                        token.getUsuarioId());
            }
            return Optional.empty();
        }

        Usuario usuario = usuarios.buscarCompleto(token.getUsuarioId()).orElseThrow();
        if (!usuario.isActivo()) {
            tokens.revocarFamilia(token.getFamiliaId(), ahora);
            return Optional.empty();
        }
        if (!token.estaRevocado()) {
            token.revocar(ahora);
        }
        return Optional.of(new Rotacion(usuario, emitir(usuario.getId(), token.getFamiliaId(), ip, userAgent)));
    }

    /** Devuelve el usuario de la sesión cerrada, si el token era conocido. */
    @Transactional
    public Optional<Long> cerrarSesion(String tokenPlano) {
        Optional<RefreshToken> token = tokens.findByTokenHash(hash(tokenPlano));
        token.ifPresent(t -> tokens.revocarFamilia(t.getFamiliaId(), reloj.instant()));
        return token.map(RefreshToken::getUsuarioId);
    }

    @Transactional
    public void revocarTodas(Long usuarioId) {
        tokens.revocarDeUsuario(usuarioId, reloj.instant());
    }

    /** Conserva la sesión desde la que se hizo el cambio. Sin token, las revoca todas. */
    @Transactional
    public void revocarOtrasSesiones(Long usuarioId, String tokenActual) {
        Optional<UUID> familia = Optional.ofNullable(tokenActual)
                .flatMap(t -> tokens.findByTokenHash(hash(t)))
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .map(RefreshToken::getFamiliaId);
        if (familia.isPresent()) {
            tokens.revocarOtrasFamilias(usuarioId, familia.get(), reloj.instant());
        } else {
            revocarTodas(usuarioId);
        }
    }

    /**
     * Dos pestañas pueden renovar a la vez con el mismo token. Si el token se rotó hace
     * instantes y la familia sigue viva, se trata como esa carrera y no como un robo.
     */
    private boolean dentroDeGracia(RefreshToken token, Instant ahora, boolean familiaActiva) {
        return familiaActiva && token.getRevocadoAt().plus(propiedades.refreshGracia()).isAfter(ahora);
    }

    private String emitir(Long usuarioId, UUID familia, String ip, String userAgent) {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String plano = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expira = reloj.instant().plus(propiedades.refreshTokenTtl());
        tokens.save(new RefreshToken(usuarioId, hash(plano), familia, expira, ip, recortar(userAgent)));
        return plano;
    }

    private String recortar(String userAgent) {
        return userAgent == null || userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
    }

    private String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
