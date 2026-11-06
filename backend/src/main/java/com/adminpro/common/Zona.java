package com.adminpro.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Las reglas de negocio con fechas usan el calendario de Colombia, no el del servidor. */
public final class Zona {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private Zona() {
    }

    public static LocalDate hoy(Clock reloj) {
        return LocalDate.now(reloj.withZone(BOGOTA));
    }
}
