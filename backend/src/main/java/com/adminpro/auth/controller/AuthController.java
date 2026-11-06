package com.adminpro.auth.controller;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.auth.dto.CambiarPasswordRequest;
import com.adminpro.auth.dto.LoginRequest;
import com.adminpro.auth.dto.TokenResponse;
import com.adminpro.auth.dto.UsuarioActualResponse;
import com.adminpro.auth.service.AuthService;
import com.adminpro.security.SecurityProperties;
import com.adminpro.security.UsuarioActual;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private static final String COOKIE = "adminpro_refresh";
    private static final String RUTA_COOKIE = "/api/v1/auth";

    private final AuthService servicio;
    private final SecurityProperties propiedades;

    public AuthController(AuthService servicio, SecurityProperties propiedades) {
        this.servicio = servicio;
        this.propiedades = propiedades;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest solicitud,
            HttpServletRequest peticion, @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String agente) {
        return responder(servicio.login(solicitud, peticion.getRemoteAddr(), agente));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public ResponseEntity<TokenResponse> refresh(@CookieValue(name = COOKIE, required = false) String token,
            HttpServletRequest peticion, @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String agente) {
        return responder(servicio.renovar(token, peticion.getRemoteAddr(), agente));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String token) {
        servicio.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public UsuarioActualResponse me() {
        return servicio.perfil(UsuarioActual.id());
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest solicitud,
            @CookieValue(name = COOKIE, required = false) String token) {
        servicio.cambiarPassword(UsuarioActual.id(), solicitud, token);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<TokenResponse> responder(AuthService.Sesion sesion) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(sesion.refreshToken(), propiedades.refreshTokenTtl()).toString())
                .body(sesion.respuesta());
    }

    private ResponseCookie cookie(String valor, Duration duracion) {
        return ResponseCookie.from(COOKIE, valor)
                .httpOnly(true)
                .secure(propiedades.cookieSecure())
                .sameSite("Strict")
                .path(RUTA_COOKIE)
                .maxAge(duracion)
                .build();
    }
}
