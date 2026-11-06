package com.adminpro.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.adminpro.exception.TipoError;

import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe Problem Details desde los filtros de seguridad, que corren antes de Spring
 * MVC y por eso no pasan por GlobalExceptionHandler. Mantiene el mismo formato.
 */
@Component
public class ProblemResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException excepcion) throws IOException {
        escribir(request, response, TipoError.NO_AUTENTICADO);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException excepcion) throws IOException {
        escribir(request, response, TipoError.SIN_PERMISO);
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response, TipoError tipo)
            throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", tipo.type().toString());
        cuerpo.put("title", tipo.title());
        cuerpo.put("status", tipo.status().value());
        cuerpo.put("detail", tipo.detalle());
        cuerpo.put("instance", request.getRequestURI());
        cuerpo.put("code", tipo.name());

        response.setStatus(tipo.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(cuerpo));
    }
}
