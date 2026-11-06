package com.adminpro.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.adminpro.support.IntegrationTestBase;

class UsuariosIT extends IntegrationTestBase {

    @Test
    void crearDevuelve201YExigeCambiarLaPassword() throws Exception {
        String correo = correoNuevo();

        crear(correo, null)
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.rol.nombre").value("Auxiliar"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/v1/auth/login").with(ipNueva()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"password\":\"Temporal.2026\"}".formatted(correo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.debeCambiarPassword").value(true));
    }

    @Test
    void correoDuplicadoSinDistinguirMayusculasEs409() throws Exception {
        crear(ADMIN.toUpperCase(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO"));
    }

    @Test
    void empleadoConUsuarioNoSePuedeVincularDosVeces() throws Exception {
        Long empleado = jdbc.queryForObject("select empleado_id from usuarios where correo = ?", Long.class, ADMIN);
        crear(correoNuevo(), empleado).andExpect(status().isConflict());
    }

    @Test
    void datosInvalidosSeñalanCadaCampo() throws Exception {
        mvc.perform(post("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"sin-arroba\",\"passwordTemporal\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("correo")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("passwordTemporal")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("rolId")));
    }

    @Test
    void versionDesactualizadaEs409() throws Exception {
        Long id = idDe(crearUsuario("AUXILIAR", false));

        actualizar(id, "Nombre uno", 0).andExpect(status().isOk());

        actualizar(id, "Nombre dos", 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_DESACTUALIZADA"));
    }

    @Test
    void unLoginNoInvalidaLaEdicionEnCurso() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);

        mvc.perform(post("/api/v1/auth/login").with(ipNueva()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"password\":\"%s\"}".formatted(correo, PASSWORD)))
                .andExpect(status().isOk());

        actualizar(idDe(correo), "Editado tras el login", 0).andExpect(status().isOk());
    }

    @Test
    void nadiePuedeDesactivarseASiMismo() throws Exception {
        mvc.perform(patch("/api/v1/usuarios/" + idDe(ADMIN) + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void listadoBuscaSinTildesYUsaElFormatoUnico() throws Exception {
        mvc.perform(get("/api/v1/usuarios").param("q", "GOMEZ")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].correo").value(ADMIN))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable").doesNotExist());
    }

    @Test
    void comodinesDeLikeSeTomanComoTexto() throws Exception {
        mvc.perform(get("/api/v1/usuarios").param("q", "%")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void ordenarPorCampoNoPermitidoEs400() throws Exception {
        mvc.perform(get("/api/v1/usuarios").param("sort", "passwordHash,asc")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    @Test
    void elTamanoDePaginaTieneTope() throws Exception {
        mvc.perform(get("/api/v1/usuarios").param("size", "5000")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void rolesMuestraLosCuatroConSusPermisos() throws Exception {
        mvc.perform(get("/api/v1/roles").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].codigo").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$[0].permisos.length()").value(13));
    }

    @Test
    void laAuditoriaRegistraQuienYQueCambioSinDatosSensibles() throws Exception {
        Long id = idDe(crearUsuario("AUXILIAR", false));

        actualizar(id, "Nombre auditado", 0).andExpect(status().isOk());

        String cuerpo = mvc.perform(get("/api/v1/auditoria")
                        .param("entidad", "USUARIO").param("entidadId", id.toString())
                        .param("accion", "ACTUALIZAR")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(SUPERVISOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].valorAnterior.nombreVisible").value("Usuario de prueba"))
                .andExpect(jsonPath("$.content[0].valorNuevo.nombreVisible").value("Nombre auditado"))
                .andExpect(jsonPath("$.content[0].usuario.nombre").value("Andrea Gómez"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("password", "$2b$");
    }

    @Test
    void siElCambioFallaNoQuedaAuditoria() throws Exception {
        Long id = idDe(crearUsuario("AUXILIAR", false));

        actualizar(id, "No debe guardarse", 99).andExpect(status().isConflict());

        Long registros = jdbc.queryForObject("select count(*) from auditoria where entidad_id = ? and accion = 'ACTUALIZAR'",
                Long.class, id);
        assertThat(registros).isZero();
    }

    @Test
    void lasColumnasDeAutorRegistranAlUsuarioAutenticado() throws Exception {
        Long id = idDe(crearUsuario("AUXILIAR", false));

        actualizar(id, "Con autor", 0).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select updated_by from usuarios where id = ?", Long.class, id))
                .isEqualTo(idDe(ADMIN));
    }

    private String correoNuevo() {
        return "nuevo-" + UUID.randomUUID() + "@adminpro.test";
    }

    private Long rolAuxiliar() {
        return jdbc.queryForObject("select id from roles where codigo = 'AUXILIAR'", Long.class);
    }

    private ResultActions crear(String correo, Long empleadoId) throws Exception {
        return mvc.perform(post("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"correo":"%s","nombreVisible":"Nuevo","rolId":%d,"empleadoId":%s,
                         "passwordTemporal":"Temporal.2026"}
                        """.formatted(correo, rolAuxiliar(), empleadoId)));
    }

    private ResultActions actualizar(Long id, String nombre, long version) throws Exception {
        return mvc.perform(put("/api/v1/usuarios/" + id).header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreVisible\":\"%s\",\"rolId\":%d,\"version\":%d}"
                        .formatted(nombre, rolAuxiliar(), version)));
    }
}
