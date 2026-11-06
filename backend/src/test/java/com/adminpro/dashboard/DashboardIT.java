package com.adminpro.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.adminpro.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

class DashboardIT extends IntegrationTestBase {

    @Test
    void elResumenCoincideConLaBaseDeDatos() throws Exception {
        Long activos = jdbc.queryForObject("select count(*) from empleados where activo", Long.class);

        mvc.perform(get("/api/v1/dashboard/resumen").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empleadosActivos").value(activos))
                .andExpect(jsonPath("$.documentosVencidos").value(greaterThan(0)))
                .andExpect(jsonPath("$.documentosPorVencer").value(greaterThan(0)));
    }

    @Test
    void losEstadosVienenCompletosYEnOrdenDelFlujo() throws Exception {
        String cuerpo = mvc.perform(get("/api/v1/dashboard/documentos-por-estado")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> estados = JsonPath.read(cuerpo, "$[*].estado");
        assertThat(estados).containsExactly("REGISTRADO", "PENDIENTE", "EN_REVISION", "APROBADO", "FINALIZADO");
    }

    @Test
    void losProximosAVencerExcluyenVencidosYFinalizados() throws Exception {
        String cuerpo = mvc.perform(get("/api/v1/dashboard/documentos-por-vencer").param("dias", "30")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> estados = JsonPath.read(cuerpo, "$.content[*].estado");
        List<Boolean> vencidos = JsonPath.read(cuerpo, "$.content[*].vencido");
        assertThat(estados).isNotEmpty().doesNotContain("FINALIZADO");
        assertThat(vencidos).doesNotContain(true);
    }

    @Test
    void diasFueraDeRangoEs400() throws Exception {
        mvc.perform(get("/api/v1/dashboard/documentos-por-vencer").param("dias", "9999")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isBadRequest());
    }

    /** Esta actividad la ven roles sin permiso de auditoría: no debe exponer valores ni usuarios. */
    @Test
    void laActividadNoExponeValoresNiEventosDeSesion() throws Exception {
        String cuerpo = mvc.perform(get("/api/v1/dashboard/actividad-reciente").param("limite", "50")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> entidades = JsonPath.read(cuerpo, "$[*].entidad");
        assertThat(entidades).isNotEmpty().allMatch(e -> e.equals("DOCUMENTO") || e.equals("EMPLEADO"));
        assertThat(cuerpo).doesNotContain("valorAnterior", "valorNuevo", "127.0.0.1");
    }

    @Test
    void lasAlertasCubrenVencidosPorVencerYResponsableInactivo() throws Exception {
        mvc.perform(get("/api/v1/dashboard/alertas").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", hasItem("DOCUMENTO_VENCIDO")))
                .andExpect(jsonPath("$[*].tipo", hasItem("DOCUMENTO_POR_VENCER")))
                .andExpect(jsonPath("$[*].tipo", hasItem("RESPONSABLE_INACTIVO")));
    }
}
