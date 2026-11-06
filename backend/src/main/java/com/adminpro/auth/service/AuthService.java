package com.adminpro.auth.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.auth.dto.CambiarPasswordRequest;
import com.adminpro.auth.dto.LoginRequest;
import com.adminpro.auth.dto.TokenResponse;
import com.adminpro.auth.dto.UsuarioActualResponse;
import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;
import com.adminpro.security.JwtService;
import com.adminpro.security.ModoDemo;
import com.adminpro.security.PasswordPolicy;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.UsuarioRepository;

/**
 * login y renovar no son transaccionales a propósito: orquestan pasos que confirman por
 * separado, para que el 401 final no revierta lo ya registrado.
 */
@Service
public class AuthService {

    private static final String ENTIDAD = "USUARIO";

    public record Sesion(TokenResponse respuesta, String refreshToken) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final IntentosLoginService intentos;
    private final AuditService auditoria;
    private final Clock reloj;
    private final ModoDemo modoDemo;
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder codificador, JwtService jwt,
            RefreshTokenService refreshTokens, IntentosLoginService intentos, AuditService auditoria, Clock reloj,
            ModoDemo modoDemo) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.intentos = intentos;
        this.auditoria = auditoria;
        this.reloj = reloj;
        this.modoDemo = modoDemo;
        this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
    }

    public Sesion login(LoginRequest solicitud, String ip, String userAgent) {
        Optional<Usuario> encontrado = usuarios.buscarPorCorreo(solicitud.correo());
        boolean passwordCorrecta = verificar(solicitud.password(), encontrado.orElse(null));

        if (encontrado.isEmpty()) {
            auditoria.registrarEvento(ENTIDAD, null, AccionAuditoria.LOGIN_FALLIDO, null);
            throw ApiException.de(TipoError.CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isActivo() || usuario.estaBloqueado(reloj.instant())) {
            auditoria.registrarEvento(ENTIDAD, usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, usuario.getId());
            throw ApiException.de(TipoError.CREDENCIALES_INVALIDAS);
        }
        if (!passwordCorrecta) {
            intentos.registrarFallo(usuario.getId());
            throw ApiException.de(TipoError.CREDENCIALES_INVALIDAS);
        }
        intentos.registrarExito(usuario.getId());
        return crearSesion(usuario, refreshTokens.emitirNuevaSesion(usuario.getId(), ip, userAgent));
    }

    public Sesion renovar(String refreshToken, String ip, String userAgent) {
        if (refreshToken == null) {
            throw ApiException.de(TipoError.NO_AUTENTICADO);
        }
        RefreshTokenService.Rotacion rotacion = refreshTokens.rotar(refreshToken, ip, userAgent)
                .orElseThrow(() -> ApiException.de(TipoError.NO_AUTENTICADO));
        return crearSesion(rotacion.usuario(), rotacion.tokenNuevo());
    }

    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        refreshTokens.cerrarSesion(refreshToken)
                .ifPresent(id -> auditoria.registrarEvento(ENTIDAD, id, AccionAuditoria.LOGOUT, id));
    }

    public UsuarioActualResponse perfil(Long usuarioId) {
        return UsuarioActualResponse.de(buscar(usuarioId));
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, CambiarPasswordRequest solicitud, String refreshToken) {
        PasswordPolicy.validar("passwordNueva", solicitud.passwordNueva());
        Usuario usuario = buscar(usuarioId);
        modoDemo.protegerCuenta(usuario.getCorreo());
        if (!verificar(solicitud.passwordActual(), usuario)) {
            throw ApiException.campo("passwordActual", "La contraseña actual no es correcta");
        }
        if (solicitud.passwordActual().equals(solicitud.passwordNueva())) {
            throw ApiException.campo("passwordNueva", "Debe ser distinta de la contraseña actual");
        }
        usuario.cambiarPassword(codificador.encode(solicitud.passwordNueva()));
        refreshTokens.revocarOtrasSesiones(usuarioId, refreshToken);
        auditoria.registrar(ENTIDAD, usuarioId, AccionAuditoria.CAMBIO_PASSWORD, null, null);
    }

    /**
     * Sin usuario se compara contra un hash ficticio: BCrypt tarda lo mismo y el tiempo de
     * respuesta no revela si el correo existe.
     */
    private boolean verificar(String password, Usuario usuario) {
        String hash = usuario == null ? hashFicticio : usuario.getPasswordHash();
        return PasswordPolicy.cabeEnBcrypt(password) && codificador.matches(password, hash) && usuario != null;
    }

    private Sesion crearSesion(Usuario usuario, String refreshToken) {
        TokenResponse respuesta = new TokenResponse(jwt.generar(usuario), jwt.segundosDeVida(),
                UsuarioActualResponse.de(usuario));
        return new Sesion(respuesta, refreshToken);
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarCompleto(id).orElseThrow(() -> ApiException.de(TipoError.NO_AUTENTICADO));
    }
}
