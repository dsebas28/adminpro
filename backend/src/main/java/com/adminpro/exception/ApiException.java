package com.adminpro.exception;

/** Error de negocio con un mensaje apto para mostrarle al usuario. */
public class ApiException extends RuntimeException {

    private final TipoError tipo;

    public ApiException(TipoError tipo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(TipoError.NO_ENCONTRADO, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(TipoError.CONFLICTO, mensaje);
    }

    public TipoError tipo() {
        return tipo;
    }
}
