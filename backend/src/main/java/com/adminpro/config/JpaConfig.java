package com.adminpro.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual")
public class JpaConfig {

    /**
     * Sin autenticación todavía no hay usuario que registrar, así que created_by y
     * updated_by quedan en NULL, igual que en las acciones del sistema. La Fase 4
     * reemplaza este bean para leer el usuario del contexto de seguridad.
     */
    @Bean
    AuditorAware<Long> auditorActual() {
        return Optional::empty;
    }
}
