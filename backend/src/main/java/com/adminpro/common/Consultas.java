package com.adminpro.common;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.adminpro.exception.ApiException;

/** Utilidades compartidas por los listados. */
public final class Consultas {

    private Consultas() {
    }

    /** Evita ordenar por campos internos, como password_hash, o por uno inexistente. */
    public static void validarOrden(Pageable paginacion, Set<String> camposPermitidos) {
        for (Sort.Order orden : paginacion.getSort()) {
            if (!camposPermitidos.contains(orden.getProperty())) {
                throw ApiException.campo("sort", "No se puede ordenar por " + orden.getProperty());
            }
        }
    }

    /** Patrón LIKE para buscar el texto en cualquier posición, tomando % y _ como literales. */
    public static String patronContiene(String texto) {
        String escapado = texto.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
