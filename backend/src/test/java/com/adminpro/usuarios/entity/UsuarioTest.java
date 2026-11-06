package com.adminpro.usuarios.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-11-06T12:00:00Z");

    @Test
    void elQuintoFalloBloqueaYReiniciaElConteo() {
        Usuario usuario = new Usuario("a@b.co", "h", "U", null, null);
        Instant hasta = AHORA.plusSeconds(900);

        for (int i = 0; i < 4; i++) {
            assertThat(usuario.registrarFallo(5, hasta)).isFalse();
        }
        assertThat(usuario.registrarFallo(5, hasta)).isTrue();

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        assertThat(usuario.estaBloqueado(hasta)).isFalse();
    }

    @Test
    void unAccesoCorrectoLimpiaElBloqueo() {
        Usuario usuario = new Usuario("a@b.co", "h", "U", null, null);
        usuario.registrarFallo(1, AHORA.plusSeconds(900));

        usuario.registrarAcceso(AHORA);

        assertThat(usuario.estaBloqueado(AHORA)).isFalse();
        assertThat(usuario.getUltimoAccesoAt()).isEqualTo(AHORA);
    }
}
