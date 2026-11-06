package com.adminpro.security;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.adminpro.exception.ApiException;

/**
 * El demo público publica las credenciales de prueba. Sin esta protección, el primer
 * visitante podría cambiar una contraseña y dejar a los demás sin acceso.
 */
@Component
public class ModoDemo {

    private static final Set<String> CUENTAS = Set.of(
            "admin@adminpro.test", "supervisor@adminpro.test", "auxiliar@adminpro.test", "consulta@adminpro.test");

    private final boolean activo;

    public ModoDemo(@Value("${adminpro.demo.activo:false}") boolean activo) {
        this.activo = activo;
    }

    public void protegerCuenta(String correo) {
        if (activo && CUENTAS.contains(correo.toLowerCase(Locale.ROOT))) {
            throw ApiException.conflicto("Esta cuenta de prueba no se puede modificar en el demo");
        }
    }
}
