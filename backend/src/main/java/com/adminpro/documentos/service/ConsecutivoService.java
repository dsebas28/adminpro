package com.adminpro.documentos.service;

import java.time.Clock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.common.Zona;

/**
 * Consecutivo DOC-AAAA-000001 (D-12). La fila de la secuencia queda bloqueada hasta el commit
 * de quien crea el documento: otra transacción espera su turno, sin duplicados. Si la primera
 * se revierte, el número se devuelve, y por eso tampoco hay huecos.
 */
@Service
public class ConsecutivoService {

    private static final String TIPO = "DOC";
    private static final String SIGUIENTE = """
            INSERT INTO secuencias_consecutivo (tipo, anio, ultimo_valor) VALUES (?, ?, 1)
            ON CONFLICT (tipo, anio) DO UPDATE
              SET ultimo_valor = secuencias_consecutivo.ultimo_valor + 1, updated_at = now()
            RETURNING ultimo_valor
            """;

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public ConsecutivoService(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** MANDATORY: fuera de una transacción el bloqueo se liberaría antes de guardar el documento. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente() {
        int anio = Zona.hoy(reloj).getYear();
        Long valor = jdbc.queryForObject(SIGUIENTE, Long.class, TIPO, anio);
        return "%s-%d-%06d".formatted(TIPO, anio, valor);
    }
}
