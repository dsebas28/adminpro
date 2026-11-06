package com.adminpro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.adminpro.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

/**
 * Matriz de la sección 6 del contrato: cada endpoint protegido contra cada rol. Los
 * identificadores no existen a propósito, para que un rol autorizado llegue al 404 o
 * 400 del servicio sin modificar datos: basta con que no reciba 401 ni 403.
 */
class PermisosIT extends IntegrationTestBase {

    private static final String A = ADMIN;
    private static final String S = SUPERVISOR;
    private static final String X = AUXILIAR;
    private static final String C = CONSULTA;
    private static final Set<String> ROLES = Set.of(A, S, X, C);

    private record Ruta(HttpMethod metodo, String ruta, Set<String> permitidos) {
    }

    private static final List<Ruta> RUTAS = List.of(
            new Ruta(HttpMethod.GET, "/api/v1/auth/me", Set.of(A, S, X, C)),
            new Ruta(HttpMethod.POST, "/api/v1/auth/cambiar-password", Set.of(A, S, X, C)),
            new Ruta(HttpMethod.GET, "/api/v1/usuarios", Set.of(A)),
            new Ruta(HttpMethod.GET, "/api/v1/usuarios/999999", Set.of(A)),
            new Ruta(HttpMethod.POST, "/api/v1/usuarios", Set.of(A)),
            new Ruta(HttpMethod.PUT, "/api/v1/usuarios/999999", Set.of(A)),
            new Ruta(HttpMethod.POST, "/api/v1/usuarios/999999/restablecer-password", Set.of(A)),
            new Ruta(HttpMethod.PATCH, "/api/v1/usuarios/999999/activar", Set.of(A)),
            new Ruta(HttpMethod.PATCH, "/api/v1/usuarios/999999/desactivar", Set.of(A)),
            new Ruta(HttpMethod.GET, "/api/v1/roles", Set.of(A)),
            new Ruta(HttpMethod.GET, "/api/v1/auditoria", Set.of(A, S)),
            new Ruta(HttpMethod.GET, "/api/v1/auditoria/999999", Set.of(A, S)));

    static Stream<Arguments> rutasPorRol() {
        return RUTAS.stream().flatMap(r -> ROLES.stream().map(rol -> Arguments.of(r, rol)));
    }

    static Stream<Ruta> rutas() {
        return RUTAS.stream();
    }

    @ParameterizedTest(name = "{0} como {1}")
    @MethodSource("rutasPorRol")
    void cadaRolRecibeLoQueLaMatrizIndica(Ruta ruta, String correo) throws Exception {
        var respuesta = mvc.perform(peticion(ruta).header(HttpHeaders.AUTHORIZATION, tokenDe(correo)));

        if (ruta.permitidos().contains(correo)) {
            int codigo = respuesta.andReturn().getResponse().getStatus();
            assertThat(codigo).as("%s debe poder entrar", correo).isNotIn(401, 403);
        } else {
            respuesta.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("SIN_PERMISO"));
        }
    }

    @ParameterizedTest(name = "{0} sin sesión")
    @MethodSource("rutas")
    void sinSesionTodoEs401(Ruta ruta) throws Exception {
        mvc.perform(peticion(ruta))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void sinPermisoElCuerpoNiSiquieraSeLee() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{json roto"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIN_PERMISO"));
    }

    @Test
    void cadaRolTieneExactamenteLosPermisosDelContrato() throws Exception {
        Map<String, List<String>> esperados = Map.of(
                A, List.of("AUDITORIA_LEER", "CATALOGOS_ESCRIBIR", "DASHBOARD_LEER", "DOCUMENTOS_APROBAR",
                        "DOCUMENTOS_DESACTIVAR", "DOCUMENTOS_ESCRIBIR", "DOCUMENTOS_LEER", "EMPLEADOS_DESACTIVAR",
                        "EMPLEADOS_ESCRIBIR", "EMPLEADOS_LEER", "USUARIOS_DESACTIVAR", "USUARIOS_ESCRIBIR",
                        "USUARIOS_LEER"),
                S, List.of("AUDITORIA_LEER", "DASHBOARD_LEER", "DOCUMENTOS_APROBAR", "DOCUMENTOS_ESCRIBIR",
                        "DOCUMENTOS_LEER", "EMPLEADOS_ESCRIBIR", "EMPLEADOS_LEER"),
                X, List.of("DASHBOARD_LEER", "DOCUMENTOS_ESCRIBIR", "DOCUMENTOS_LEER", "EMPLEADOS_ESCRIBIR",
                        "EMPLEADOS_LEER"),
                C, List.of("DASHBOARD_LEER", "DOCUMENTOS_LEER", "EMPLEADOS_LEER"));

        for (var esperado : esperados.entrySet()) {
            String cuerpo = mvc.perform(get("/api/v1/auth/me")
                            .header(HttpHeaders.AUTHORIZATION, tokenDe(esperado.getKey())))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> permisos = JsonPath.read(cuerpo, "$.permisos");
            assertThat(permisos).as(esperado.getKey()).isEqualTo(esperado.getValue());
        }
    }

    private MockHttpServletRequestBuilder peticion(Ruta ruta) {
        var peticion = request(ruta.metodo(), ruta.ruta());
        if (ruta.metodo() == HttpMethod.POST || ruta.metodo() == HttpMethod.PUT) {
            peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return peticion;
    }
}
