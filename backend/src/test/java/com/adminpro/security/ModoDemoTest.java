package com.adminpro.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.adminpro.exception.ApiException;

class ModoDemoTest {

    @Test
    void enElDemoLasCuentasDePruebaEstanProtegidas() {
        ModoDemo demo = new ModoDemo(true);

        assertThatThrownBy(() -> demo.protegerCuenta("ADMIN@adminpro.test")).isInstanceOf(ApiException.class);
    }

    @Test
    void enElDemoLosUsuariosCreadosPorVisitantesSiSeModifican() {
        assertThatCode(() -> new ModoDemo(true).protegerCuenta("otro@adminpro.test")).doesNotThrowAnyException();
    }

    @Test
    void fueraDelDemoNoHayRestriccion() {
        assertThatCode(() -> new ModoDemo(false).protegerCuenta("admin@adminpro.test")).doesNotThrowAnyException();
    }
}
