package com.adminpro.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("adminpro.security")
public record SecurityProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration refreshGracia,
        boolean cookieSecure,
        int maxIntentos,
        Duration bloqueo,
        int loginPorMinuto) {
}
