package com.adminpro.security;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.adminpro.exception.TipoError;

/** Con una contraseña temporal, solo se permite cambiarla, cerrar sesión y consultar el perfil. */
public class CambioPasswordFilter extends OncePerRequestFilter {

    private static final Set<String> PERMITIDAS = Set.of(
            "/api/v1/auth/cambiar-password",
            "/api/v1/auth/logout",
            "/api/v1/auth/me");

    private final ProblemResponder responder;

    public CambioPasswordFilter(ProblemResponder responder) {
        this.responder = responder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken token
                && Boolean.TRUE.equals(token.getToken().getClaimAsBoolean(JwtService.CLAIM_CAMBIO_PASSWORD))
                && !PERMITIDAS.contains(request.getRequestURI())) {
            responder.escribir(request, response, TipoError.CAMBIO_PASSWORD_REQUERIDO);
            return;
        }
        cadena.doFilter(request, response);
    }
}
