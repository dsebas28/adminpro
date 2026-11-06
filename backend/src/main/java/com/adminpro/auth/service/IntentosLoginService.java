package com.adminpro.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.security.SecurityProperties;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.UsuarioRepository;

/**
 * Cada método confirma su propia transacción. Es un servicio aparte porque AuthService
 * termina lanzando un 401, y si el conteo ocurriera en esa misma transacción se revertiría.
 */
@Service
public class IntentosLoginService {

    private static final String ENTIDAD = "USUARIO";

    private final UsuarioRepository usuarios;
    private final AuditService auditoria;
    private final SecurityProperties propiedades;
    private final Clock reloj;

    public IntentosLoginService(UsuarioRepository usuarios, AuditService auditoria,
            SecurityProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public void registrarFallo(Long usuarioId) {
        Usuario usuario = usuarios.buscarParaActualizar(usuarioId).orElseThrow();
        Instant bloqueoHasta = reloj.instant().plus(propiedades.bloqueo());
        boolean bloqueada = usuario.registrarFallo(propiedades.maxIntentos(), bloqueoHasta);
        auditoria.registrarEvento(ENTIDAD, usuarioId,
                bloqueada ? AccionAuditoria.CUENTA_BLOQUEADA : AccionAuditoria.LOGIN_FALLIDO, usuarioId);
    }

    @Transactional
    public void registrarExito(Long usuarioId) {
        Usuario usuario = usuarios.buscarParaActualizar(usuarioId).orElseThrow();
        usuario.registrarAcceso(reloj.instant());
        auditoria.registrarEvento(ENTIDAD, usuarioId, AccionAuditoria.LOGIN_OK, usuarioId);
    }
}
