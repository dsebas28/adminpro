package com.adminpro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

import com.adminpro.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

class AuthIT extends IntegrationTestBase {

    private static final String COOKIE = "adminpro_refresh";

    @Test
    void loginEntregaTokenUtilYCookieProtegida() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);

        MockHttpServletResponse respuesta = login(correo, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.usuario.rol").value("AUXILIAR"))
                .andReturn().getResponse();

        String cookie = respuesta.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=604800");

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(respuesta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(correo));
    }

    @Test
    void elLoginAceptaElHashDeLosDatosDePrueba() throws Exception {
        login(CONSULTA, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void correoInexistenteYPasswordIncorrectaResponden_igual() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);

        String existente = login(correo, "incorrecta-1234").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String inexistente = login("nadie@adminpro.test", "incorrecta-1234").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(existente).isEqualTo(inexistente);
    }

    @Test
    void passwordMayorA72BytesEs401YNoError500() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        login(correo, "ñ".repeat(100)).andExpect(status().isUnauthorized());
    }

    @Test
    void cincoFallosBloqueanAunqueLaPasswordSeaCorrecta() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        for (int i = 0; i < 5; i++) {
            login(correo, "incorrecta-1234").andExpect(status().isUnauthorized());
        }

        login(correo, PASSWORD).andExpect(status().isUnauthorized());
        assertThat(contarAuditoria(correo, "CUENTA_BLOQUEADA")).isEqualTo(1);

        jdbc.update("update usuarios set bloqueado_hasta = now() - interval '1 minute' where correo = ?", correo);
        login(correo, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unLoginCorrectoReiniciaElConteoDeFallos() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        for (int i = 0; i < 4; i++) {
            login(correo, "incorrecta-1234");
        }
        login(correo, PASSWORD).andExpect(status().isOk());
        login(correo, "incorrecta-1234");

        login(correo, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void elLimitePorIpResponde429ConRetryAfter() throws Exception {
        String direccion = "10.9.9.9";
        for (int i = 0; i < 10; i++) {
            intentar("nadie@adminpro.test", "x", direccion).andExpect(status().isUnauthorized());
        }
        intentar("nadie@adminpro.test", "x", direccion)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("DEMASIADOS_INTENTOS"));
    }

    @Test
    void refreshRotaElToken() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        Cookie primera = cookieDe(login(correo, PASSWORD).andReturn().getResponse());

        MockHttpServletResponse respuesta = refresh(primera).andExpect(status().isOk()).andReturn().getResponse();
        Cookie segunda = cookieDe(respuesta);

        assertThat(segunda.getValue()).isNotEqualTo(primera.getValue());
        refresh(segunda).andExpect(status().isOk());
    }

    @Test
    void elTokenSeGuardaSoloComoHash() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        String plano = cookieDe(login(correo, PASSWORD).andReturn().getResponse()).getValue();

        Long iguales = jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Long.class, plano);
        assertThat(iguales).isZero();
    }

    @Test
    void dosPestanasConElMismoTokenNoCierranLaSesion() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        Cookie primera = cookieDe(login(correo, PASSWORD).andReturn().getResponse());

        refresh(primera).andExpect(status().isOk());
        refresh(primera).andExpect(status().isOk());

        assertThat(contarAuditoria(correo, "REUTILIZACION_TOKEN")).isZero();
    }

    @Test
    void reutilizarUnTokenViejoRevocaTodaLaFamilia() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        Cookie robada = cookieDe(login(correo, PASSWORD).andReturn().getResponse());
        Cookie legitima = cookieDe(refresh(robada).andReturn().getResponse());
        jdbc.update("""
                update refresh_tokens set revocado_at = now() - interval '1 minute'
                where revocado_at is not null and usuario_id = ?
                """, idDe(correo));

        refresh(robada).andExpect(status().isUnauthorized());

        refresh(legitima).andExpect(status().isUnauthorized());
        assertThat(contarAuditoria(correo, "REUTILIZACION_TOKEN")).isEqualTo(1);
    }

    @Test
    void logoutRevocaLaSesionYBorraLaCookie() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        Cookie cookie = cookieDe(login(correo, PASSWORD).andReturn().getResponse());

        String borrado = mvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(borrado).contains("Max-Age=0");
        refresh(cookie).andExpect(status().isUnauthorized());
        assertThat(contarAuditoria(correo, "REUTILIZACION_TOKEN")).isZero();
    }

    @Test
    void refreshSinCookieEs401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void tokenAlteradoEs401() throws Exception {
        String token = tokenDe(ADMIN);
        String alterado = token.substring(0, token.length() - 4) + "AAAA";

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void passwordTemporalObligaACambiarlaAntesDeContinuar() throws Exception {
        String correo = crearUsuario("ADMINISTRADOR", true);
        MockHttpServletResponse sesion = login(correo, PASSWORD)
                .andExpect(jsonPath("$.usuario.debeCambiarPassword").value(true))
                .andReturn().getResponse();
        String token = bearer(sesion);

        mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CAMBIO_PASSWORD_REQUERIDO"));

        cambiarPassword(token, cookieDe(sesion), PASSWORD, "NuevaClave.2026").andExpect(status().isNoContent());

        MockHttpServletResponse renovada = refresh(cookieDe(sesion)).andExpect(status().isOk())
                .andReturn().getResponse();
        mvc.perform(get("/api/v1/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(renovada)))
                .andExpect(status().isOk());
        login(correo, "NuevaClave.2026").andExpect(status().isOk());
    }

    @Test
    void cambiarPasswordRevocaLasOtrasSesiones() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        MockHttpServletResponse actual = login(correo, PASSWORD).andReturn().getResponse();
        Cookie otra = cookieDe(login(correo, PASSWORD).andReturn().getResponse());

        cambiarPassword(bearer(actual), cookieDe(actual), PASSWORD, "NuevaClave.2026")
                .andExpect(status().isNoContent());

        refresh(otra).andExpect(status().isUnauthorized());
        refresh(cookieDe(actual)).andExpect(status().isOk());
    }

    @Test
    void passwordActualIncorrectaSeñalaElCampo() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        MockHttpServletResponse sesion = login(correo, PASSWORD).andReturn().getResponse();

        cambiarPassword(bearer(sesion), cookieDe(sesion), "incorrecta-1234", "NuevaClave.2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("passwordActual"));
    }

    /** 60 caracteres pasan @Size, pero con ñ ocupan 120 bytes, más de lo que admite BCrypt. */
    @Test
    void passwordNuevaDeMasDe72BytesEs400YNoError500() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        MockHttpServletResponse sesion = login(correo, PASSWORD).andReturn().getResponse();

        cambiarPassword(bearer(sesion), cookieDe(sesion), PASSWORD, "ñ".repeat(60))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("passwordNueva"));
    }

    @Test
    void desactivarAUnUsuarioCortaSuRenovacionYSuLogin() throws Exception {
        String correo = crearUsuario("AUXILIAR", false);
        Cookie cookie = cookieDe(login(correo, PASSWORD).andReturn().getResponse());

        mvc.perform(patch("/api/v1/usuarios/" + idDe(correo) + "/desactivar")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk());

        refresh(cookie).andExpect(status().isUnauthorized());
        login(correo, PASSWORD).andExpect(status().isUnauthorized());
    }

    private ResultActions login(String correo, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(ipNueva())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoLogin(correo, password)));
    }

    private ResultActions intentar(String correo, String password, String direccion) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(ip(direccion))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoLogin(correo, password)));
    }

    private String cuerpoLogin(String correo, String password) {
        return "{\"correo\":\"%s\",\"password\":\"%s\"}".formatted(correo, password);
    }

    private ResultActions refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie));
    }

    private ResultActions cambiarPassword(String token, Cookie cookie, String actual, String nueva) throws Exception {
        return mvc.perform(post("/api/v1/auth/cambiar-password")
                .header(HttpHeaders.AUTHORIZATION, token)
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"%s\",\"passwordNueva\":\"%s\"}".formatted(actual, nueva)));
    }

    private Cookie cookieDe(MockHttpServletResponse respuesta) {
        return respuesta.getCookie(COOKIE);
    }

    private String bearer(MockHttpServletResponse respuesta) throws Exception {
        return "Bearer " + JsonPath.read(respuesta.getContentAsString(), "$.accessToken");
    }

    private Long contarAuditoria(String correo, String accion) {
        return jdbc.queryForObject("select count(*) from auditoria where entidad_id = ? and accion = ?",
                Long.class, idDe(correo), accion);
    }
}
