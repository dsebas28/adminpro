package com.adminpro.exception;

import java.util.List;

/** Error de negocio con un mensaje apto para mostrarle al usuario. */
public class ApiException extends RuntimeException {

    private final TipoError tipo;
    private final List<ErrorCampo> errores;

    public ApiException(TipoError tipo, String mensaje) {
        this(tipo, mensaje, List.of());
    }

    private ApiException(TipoError tipo, String mensaje, List<ErrorCampo> errores) {
        super(mensaje);
        this.tipo = tipo;
        this.errores = errores;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(TipoError.NO_ENCONTRADO, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(TipoError.CONFLICTO, mensaje);
    }

    /** Un duplicado es un conflicto (409), pero el formulario necesita saber en qué campo. */
    public static ApiException conflictoEn(String campo, String mensaje) {
        return new ApiException(TipoError.CONFLICTO, TipoError.CONFLICTO.detalle(),
                List.of(new ErrorCampo(campo, mensaje)));
    }

    public static ApiException de(TipoError tipo) {
        return new ApiException(tipo, tipo.detalle());
    }

    public static ApiException campo(String campo, String mensaje) {
        return new ApiException(TipoError.VALIDACION, TipoError.VALIDACION.detalle(),
                List.of(new ErrorCampo(campo, mensaje)));
    }

    public TipoError tipo() {
        return tipo;
    }

    public List<ErrorCampo> errores() {
        return errores;
    }
}
