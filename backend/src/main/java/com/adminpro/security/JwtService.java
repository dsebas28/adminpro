package com.adminpro.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.adminpro.usuarios.entity.Usuario;

/** Emite los access tokens. Spring Security se encarga de validarlos. */
@Service
public class JwtService {

    public static final String EMISOR = "adminpro";
    public static final String CLAIM_PERMISOS = "permisos";
    public static final String CLAIM_CAMBIO_PASSWORD = "pwd_change";

    private final JwtEncoder encoder;
    private final SecurityProperties propiedades;
    private final Clock reloj;

    public JwtService(JwtEncoder encoder, SecurityProperties propiedades, Clock reloj) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public String generar(Usuario usuario) {
        Instant ahora = reloj.instant();
        List<String> permisos = usuario.getRol().codigosPermisos();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.accessTokenTtl()))
                .claim("rol", usuario.getRol().getCodigo())
                .claim(CLAIM_PERMISOS, permisos)
                .claim(CLAIM_CAMBIO_PASSWORD, usuario.isDebeCambiarPassword())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    public long segundosDeVida() {
        return propiedades.accessTokenTtl().toSeconds();
    }
}
