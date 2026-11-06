package com.adminpro.common;

/** Referencia corta a otra entidad, para no anidar objetos completos en las respuestas. */
public record RefResponse(Long id, String nombre) {
}
