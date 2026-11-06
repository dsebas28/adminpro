package com.adminpro.empleados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.adminpro.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

class EmpleadosIT extends IntegrationTestBase {

    private static final AtomicLong CEDULAS = new AtomicLong(5000000000L);

    @Test
    void crearConsultarYEditar() throws Exception {
        String cedula = cedula();
        long id = idDe(crear(cedula, cedula + "@adminpro.test", "2024-02-01").andExpect(status().isCreated()));

        mvc.perform(get("/api/v1/empleados/" + id).header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cedula").value(cedula))
                .andExpect(jsonPath("$.departamento.nombre").value("Compras"))
                .andExpect(jsonPath("$.cargo.nombre").value("Analista"));

        editar(id, cedula, cedula + "@adminpro.test", "Otro Nombre", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Otro Nombre"));
    }

    @Test
    void cedulaDuplicadaEs409ConElCampo() throws Exception {
        crear("1000200301", cedula() + "@adminpro.test", "2024-02-01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("cedula"));
    }

    @Test
    void correoDuplicadoSinImportarMayusculasEs409() throws Exception {
        crear(cedula(), "ANDREA.GOMEZ@adminpro.test", "2024-02-01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("correo"));
    }

    @Test
    void cedulaConLetrasEs400() throws Exception {
        crear("12A456", cedula() + "@adminpro.test", "2024-02-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cedula"));
    }

    @Test
    void fechaDeIngresoFuturaEs400() throws Exception {
        String manana = LocalDate.now().plusDays(3).toString();

        crear(cedula(), cedula() + "@adminpro.test", manana)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fechaIngreso"));
    }

    @Test
    void versionDesactualizadaEs409() throws Exception {
        String cedula = cedula();
        long id = idDe(crear(cedula, cedula + "@adminpro.test", "2024-02-01"));
        editar(id, cedula, cedula + "@adminpro.test", "Uno", 0).andExpect(status().isOk());

        editar(id, cedula, cedula + "@adminpro.test", "Dos", 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_DESACTUALIZADA"));
    }

    @Test
    void buscaSinTildesPorNombreCedulaOCorreo() throws Exception {
        String token = tokenDe(CONSULTA);

        mvc.perform(get("/api/v1/empleados").param("q", "GOMEZ RESTREPO").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/empleados").param("q", "1000200301").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.content[0].apellidos").value("Gómez Restrepo"));
    }

    @Test
    void filtraPorDepartamentoYEstado() throws Exception {
        Long gerencia = jdbc.queryForObject("select id from departamentos where nombre = 'Gerencia'", Long.class);

        mvc.perform(get("/api/v1/empleados").param("departamentoId", gerencia.toString()).param("activo", "true")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void ordenarPorCampoNoPermitidoEs400() throws Exception {
        mvc.perform(get("/api/v1/empleados").param("sort", "version,asc")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elHistorialMuestraLosCambiosSinLaIp() throws Exception {
        String cedula = cedula();
        long id = idDe(crear(cedula, cedula + "@adminpro.test", "2024-02-01"));
        editar(id, cedula, cedula + "@adminpro.test", "Nombre Nuevo", 0).andExpect(status().isOk());

        mvc.perform(get("/api/v1/empleados/" + id + "/historial").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].accion").value("ACTUALIZAR"))
                .andExpect(jsonPath("$.content[0].valorAnterior.nombres").value("Prueba"))
                .andExpect(jsonPath("$.content[0].valorNuevo.nombres").value("Nombre Nuevo"))
                .andExpect(jsonPath("$.content[0].ip").doesNotExist());
    }

    @Test
    void desactivarAlEmpleadoDesactivaSuUsuario() throws Exception {
        String cedula = cedula();
        long id = idDe(crear(cedula, cedula + "@adminpro.test", "2024-02-01"));
        String correo = crearUsuario("AUXILIAR", false);
        jdbc.update("update usuarios set empleado_id = ? where correo = ?", id, correo);

        mvc.perform(patch("/api/v1/empleados/" + id + "/desactivar").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        assertThat(jdbc.queryForObject("select activo from usuarios where correo = ?", Boolean.class, correo)).isFalse();
    }

    @Test
    void reactivarAlEmpleadoNoReactivaSuUsuario() throws Exception {
        String cedula = cedula();
        long id = idDe(crear(cedula, cedula + "@adminpro.test", "2024-02-01"));
        String token = tokenDe(ADMIN);
        mvc.perform(patch("/api/v1/empleados/" + id + "/desactivar").header(HttpHeaders.AUTHORIZATION, token));

        mvc.perform(patch("/api/v1/empleados/" + id + "/activar").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void noPuedesDesactivarAlEmpleadoDeTuPropioUsuario() throws Exception {
        Long propio = jdbc.queryForObject("select empleado_id from usuarios where correo = ?", Long.class, ADMIN);

        mvc.perform(patch("/api/v1/empleados/" + propio + "/desactivar").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isConflict());
    }

    private String cedula() {
        return Long.toString(CEDULAS.incrementAndGet());
    }

    private ResultActions crear(String cedula, String correo, String fecha) throws Exception {
        return mvc.perform(post("/api/v1/empleados").header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(cedula, correo, "Prueba", fecha, null)));
    }

    private ResultActions editar(long id, String cedula, String correo, String nombres, long version) throws Exception {
        return mvc.perform(put("/api/v1/empleados/" + id).header(HttpHeaders.AUTHORIZATION, tokenDe(SUPERVISOR))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(cedula, correo, nombres, "2024-02-01", version)));
    }

    private String cuerpo(String cedula, String correo, String nombres, String fecha, Long version) {
        Long departamento = jdbc.queryForObject("select id from departamentos where nombre = 'Compras'", Long.class);
        Long cargo = jdbc.queryForObject("select id from cargos where nombre = 'Analista'", Long.class);
        return """
                {"cedula":"%s","nombres":"%s","apellidos":"Empleada","correo":"%s","telefono":"3005550000",
                 "fechaIngreso":"%s","departamentoId":%d,"cargoId":%d,"version":%s}
                """.formatted(cedula, nombres, correo, fecha, departamento, cargo, version);
    }

    private long idDe(ResultActions respuesta) throws Exception {
        return ((Number) JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
