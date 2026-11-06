package com.adminpro.support;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.adminpro.security.JwtService;
import com.adminpro.usuarios.repository.UsuarioRepository;

/**
 * Base de las pruebas de integración: un PostgreSQL 16 real. El contenedor se inicia una
 * sola vez para toda la suite, porque Spring reutiliza el contexto entre clases de prueba
 * y un contenedor por clase lo dejaría conectado a una base que ya no existe.
 */
@SpringBootTest(properties = {
        "JWT_SECRET=secreto-solo-para-pruebas-de-integracion-0123456789",
        "COOKIE_SECURE=false",
        "ADJUNTOS_DIR=${java.io.tmpdir}/adminpro-pruebas"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "AdminPro.Demo2026";
    protected static final String ADMIN = "admin@adminpro.test";
    protected static final String SUPERVISOR = "supervisor@adminpro.test";
    protected static final String AUXILIAR = "auxiliar@adminpro.test";
    protected static final String CONSULTA = "consulta@adminpro.test";

    private static final AtomicInteger IPS = new AtomicInteger();

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private JwtService jwt;

    @Autowired
    private UsuarioRepository usuarios;

    protected String tokenDe(String correo) {
        return "Bearer " + jwt.generar(usuarios.buscarPorCorreo(correo).orElseThrow());
    }

    /** Cada prueba usa su propio usuario para no heredar intentos ni sesiones de otra. */
    protected String crearUsuario(String rol, boolean debeCambiarPassword) {
        String correo = "prueba-" + UUID.randomUUID() + "@adminpro.test";
        jdbc.update("""
                insert into usuarios (correo, password_hash, nombre_visible, rol_id, debe_cambiar_password)
                select ?, (select password_hash from usuarios where correo = ?), 'Usuario de prueba', r.id, ?
                from roles r where r.codigo = ?
                """, correo, ADMIN, debeCambiarPassword, rol);
        return correo;
    }

    protected Long idDe(String correo) {
        return jdbc.queryForObject("select id from usuarios where correo = ?", Long.class, correo);
    }

    /** Una IP distinta por llamada, para que el límite de intentos no cruce pruebas. */
    protected RequestPostProcessor ipNueva() {
        return ip("10.1." + IPS.incrementAndGet() / 250 + "." + (IPS.get() % 250 + 1));
    }

    protected RequestPostProcessor ip(String direccion) {
        return peticion -> {
            peticion.setRemoteAddr(direccion);
            return peticion;
        };
    }
}
