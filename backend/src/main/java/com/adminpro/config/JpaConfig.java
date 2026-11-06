package com.adminpro.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.adminpro.security.UsuarioActual;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorActual")
public class JpaConfig {

    /** Sin usuario autenticado, created_by y updated_by quedan en NULL: es una acción del sistema. */
    @Bean
    AuditorAware<Long> auditorActual() {
        return UsuarioActual::idOpcional;
    }

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
