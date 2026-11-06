package com.adminpro.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Formato único de los listados. Evita serializar Page directamente, cuya forma
 * JSON depende de la versión de Spring y rompería el contrato con el frontend.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> pagina, Function<E, T> mapper) {
        return new PageResponse<>(
                pagina.getContent().stream().map(mapper).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
