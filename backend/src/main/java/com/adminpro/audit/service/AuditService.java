package com.adminpro.audit.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.entity.Auditoria;
import com.adminpro.audit.repository.AuditoriaRepository;

/**
 * Registra cambios dentro de la transacción de quien llama (D-11): si el cambio se
 * revierte, su auditoría también. Quien llama elige qué campos incluir en los valores,
 * de modo que las contraseñas y los hashes nunca llegan aquí.
 */
@Service
public class AuditService {

    private final AuditoriaRepository repositorio;

    public AuditService(AuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Cambio hecho por el usuario autenticado. */
    @Transactional
    public void registrar(String entidad, Long entidadId, AccionAuditoria accion,
            Map<String, Object> anterior, Map<String, Object> nuevo) {
        repositorio.save(new Auditoria(entidad, entidadId, accion, anterior, nuevo, ipActual(), null));
    }

    /** Evento sin sesión, como un login: el autor se indica de forma explícita. */
    @Transactional
    public void registrarEvento(String entidad, Long entidadId, AccionAuditoria accion, Long autor) {
        repositorio.save(new Auditoria(entidad, entidadId, accion, null, null, ipActual(), autor));
    }

    private String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRemoteAddr();
        }
        return null;
    }
}
