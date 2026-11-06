package com.adminpro.exception;

/** Error de validación de un campo, dentro de la lista "errors" de Problem Details. */
public record ErrorCampo(String field, String message) {
}
