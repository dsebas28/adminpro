package com.adminpro.common.catalogo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.adminpro.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

class CatalogosIT extends IntegrationTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"departamentos", "cargos", "categorias-documento"})
    void cicloCompleto(String catalogo) throws Exception {
        String ruta = "/api/v1/" + catalogo;
        String nombre = nombre();
        ResultActions creado = crear(ruta, nombre)
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION));
        long id = ((Number) JsonPath.read(creado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(put(ruta + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s editado\",\"version\":0}".formatted(nombre)))
                .andExpect(status().isOk());

        mvc.perform(put(ruta + "/" + id).header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"otro %s\",\"version\":0}".formatted(nombre)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_DESACTUALIZADA"));

        mvc.perform(patch(ruta + "/" + id + "/desactivar").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(patch(ruta + "/" + id + "/activar").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @ParameterizedTest
    @CsvSource({"departamentos,GERENCIA", "cargos,ANALISTA", "categorias-documento,ACTAS"})
    void nombreDuplicadoSinImportarMayusculasEs409(String catalogo, String nombre) throws Exception {
        crear("/api/v1/" + catalogo, nombre).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @CsvSource({
            "departamentos,select id from departamentos where nombre = 'Gerencia'",
            "cargos,select id from cargos where nombre = 'Analista'",
            "categorias-documento,select id from categorias_documento where nombre = 'Contratos'"
    })
    void noSeDesactivaUnCatalogoEnUso(String catalogo, String consulta) throws Exception {
        Long id = jdbc.queryForObject(consulta, Long.class);

        mvc.perform(patch("/api/v1/" + catalogo + "/" + id + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"departamentos", "cargos", "categorias-documento"})
    void listadoBuscaSinTildesYPagina(String catalogo) throws Exception {
        mvc.perform(get("/api/v1/" + catalogo).param("activo", "true").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    private ResultActions crear(String ruta, String nombre) throws Exception {
        return mvc.perform(post(ruta).header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"%s\",\"descripcion\":\"Prueba\"}".formatted(nombre)));
    }

    private String nombre() {
        return "Catálogo " + UUID.randomUUID();
    }
}
