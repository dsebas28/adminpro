package com.adminpro.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Acceso al usuario autenticado de la petición en curso. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Optional<Long> idOpcional() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken token) {
            return Optional.of(Long.valueOf(token.getToken().getSubject()));
        }
        return Optional.empty();
    }

    public static boolean tiene(String permiso) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> permiso.equals(autoridad.getAuthority()));
    }

    public static Long id() {
        return idOpcional().orElseThrow(() -> new IllegalStateException("No hay usuario autenticado"));
    }
}
