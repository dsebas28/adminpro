package com.adminpro.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Devuelve el demo público a sus datos de prueba, cada noche y en cada arranque. Lo segundo
 * cubre los planes gratuitos, donde el servidor se apaga por inactividad y no llega a la
 * hora programada. Solo existe en el perfil demo: nunca corre en desarrollo ni en producción.
 */
@Configuration
@Profile("demo")
@EnableScheduling
public class DemoReset implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoReset.class);

    private final JdbcTemplate jdbc;
    private final DataSource fuente;
    private final TransactionTemplate transaccion;

    public DemoReset(JdbcTemplate jdbc, DataSource fuente, TransactionTemplate transaccion) {
        this.jdbc = jdbc;
        this.fuente = fuente;
        this.transaccion = transaccion;
    }

    @Override
    @Order(1)
    public void run(ApplicationArguments argumentos) {
        restaurar();
    }

    @Scheduled(cron = "${adminpro.demo.reset-cron:0 0 3 * * *}", zone = "America/Bogota")
    public void restaurar() {
        transaccion.executeWithoutResult(estado -> {
            // TRUNCATE porque auditoría e historial rechazan DELETE. Roles y permisos no se tocan.
            jdbc.execute("""
                    TRUNCATE auditoria, documento_historial, documento_adjuntos, documentos,
                             secuencias_consecutivo, refresh_tokens RESTART IDENTITY
                    """);
            jdbc.update("UPDATE usuarios SET empleado_id = NULL");
            jdbc.update("DELETE FROM empleados");
            jdbc.update("DELETE FROM cargos");
            jdbc.update("DELETE FROM departamentos");
            jdbc.update("DELETE FROM categorias_documento");
            jdbc.update("DELETE FROM usuarios");

            ResourceDatabasePopulator datos = new ResourceDatabasePopulator(
                    new ClassPathResource("db/seed/R__datos_prueba.sql"));
            datos.setSqlScriptEncoding("UTF-8");
            datos.execute(fuente);
        });
        log.info("Datos del demo restaurados");
    }
}
