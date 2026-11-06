package com.adminpro.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Orígenes autorizados, separados por coma en la variable CORS_ALLOWED_ORIGINS. */
@ConfigurationProperties("adminpro.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
