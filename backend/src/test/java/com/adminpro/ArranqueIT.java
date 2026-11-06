package com.adminpro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;

import com.adminpro.support.IntegrationTestBase;

class ArranqueIT extends IntegrationTestBase {

    @Test
    void healthEsPublicoYRespondeUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void swaggerEsPublicoEnDev() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AdminPro API"));
    }

    @Test
    void rutaInexistenteConSesionDevuelveProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/no-existe").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    void elPerfilDevCargaLosDatosDePrueba() {
        assertThat(jdbc.queryForObject("select count(*) from empleados", Long.class)).isEqualTo(26);
    }

    /** Con la imagen Linux, lower() también debe convertir letras con tilde. */
    @Test
    void laUnicidadDeNombresIgnoraMayusculasConTilde() {
        assertThatThrownBy(() -> jdbc.update("insert into departamentos (nombre) values ('TESORERÍA')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
