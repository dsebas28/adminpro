package com.adminpro.security;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import com.adminpro.exception.TipoError;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

/**
 * Límite de intentos por IP en el login. Las cubetas viven en una caché con vencimiento
 * para que un atacante con muchas IP no pueda agotar la memoria del servidor.
 */
public class LimiteLoginFilter extends OncePerRequestFilter {

    static final String RUTA_LOGIN = "/api/v1/auth/login";

    private final Cache<String, Bucket> cubetas = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();
    private final int intentosPorMinuto;
    private final ProblemResponder responder;

    public LimiteLoginFilter(int intentosPorMinuto, ProblemResponder responder) {
        this.intentosPorMinuto = intentosPorMinuto;
        this.responder = responder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && RUTA_LOGIN.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Bucket cubeta = cubetas.get(request.getRemoteAddr(), ip -> crearCubeta());
        ConsumptionProbe intento = cubeta.tryConsumeAndReturnRemaining(1);
        if (intento.isConsumed()) {
            cadena.doFilter(request, response);
            return;
        }
        long segundos = Duration.ofNanos(intento.getNanosToWaitForRefill()).toSeconds() + 1;
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundos));
        responder.escribir(request, response, TipoError.DEMASIADOS_INTENTOS);
    }

    private Bucket crearCubeta() {
        return Bucket.builder()
                .addLimit(limite -> limite.capacity(intentosPorMinuto)
                        .refillIntervally(intentosPorMinuto, Duration.ofMinutes(1)))
                .build();
    }
}
