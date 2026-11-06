package com.adminpro.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.adminpro.config.CorsProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final int BYTES_MINIMOS_SECRETO = 32; // HS256 exige al menos 256 bits.

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ProblemResponder responder,
            SecurityProperties propiedades, CorsConfigurationSource configuracionCors) throws Exception {
        http
                // Sin sesión ni cookies de autenticación en la API: la única cookie, la del
                // refresh token, es SameSite=Strict y por eso no aplica el ataque CSRF.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(configuracionCors))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDePermisos()))
                        .authenticationEntryPoint(responder)
                        .accessDeniedHandler(responder))
                .exceptionHandling(e -> e.authenticationEntryPoint(responder).accessDeniedHandler(responder))
                .addFilterBefore(new LimiteLoginFilter(propiedades.loginPorMinuto(), responder),
                        BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new CambioPasswordFilter(responder), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private JwtAuthenticationConverter convertidorDePermisos() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(JwtService.CLAIM_PERMISOS);
        autoridades.setAuthorityPrefix("");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    @Bean
    CorsConfigurationSource configuracionCors(CorsProperties propiedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propiedades.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(true); // Necesario para la cookie del refresh token.
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    @Bean
    PasswordEncoder codificadorDePasswords() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey claveJwt(SecurityProperties propiedades) {
        byte[] secreto = propiedades.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secreto.length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
        return new SecretKeySpec(secreto, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    JwtDecoder decodificadorJwt(SecretKey claveJwt) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        return decodificador;
    }
}
