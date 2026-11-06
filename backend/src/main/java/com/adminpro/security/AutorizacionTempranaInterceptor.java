package com.adminpro.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Evalúa el @PreAuthorize del controlador antes de leer el cuerpo de la petición.
 * Spring resuelve los argumentos, incluido @Valid, antes de la seguridad de métodos: sin
 * esto, un rol sin permiso que enviara datos inválidos recibiría 400 con los detalles de
 * validación, y no 403. La regla sigue definida en un solo lugar, la anotación.
 */
public class AutorizacionTempranaInterceptor implements HandlerInterceptor {

    private final PreAuthorizeAuthorizationManager autorizador = new PreAuthorizeAuthorizationManager();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod metodo) {
            Supplier<Authentication> autenticacion = () -> SecurityContextHolder.getContext().getAuthentication();
            AuthorizationResult resultado = autorizador.authorize(autenticacion,
                    new SimpleMethodInvocation(metodo.getBean(), metodo.getMethod()));
            if (resultado != null && !resultado.isGranted()) {
                throw new AccessDeniedException("Acceso denegado");
            }
        }
        return true;
    }
}
