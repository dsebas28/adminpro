package com.adminpro.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;

/** Catálogo de errores de la API: da el type, title, status y code de cada Problem Details. */
public enum TipoError {

    VALIDACION(HttpStatus.BAD_REQUEST, "validacion", "Datos inválidos",
            "La solicitud contiene errores de validación."),
    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST, "solicitud-invalida", "Solicitud inválida",
            "La solicitud no tiene el formato esperado."),
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "no-autenticado", "No autenticado",
            "Debes iniciar sesión para continuar."),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", "Credenciales inválidas",
            "Correo o contraseña incorrectos, o la cuenta está bloqueada temporalmente."),
    SIN_PERMISO(HttpStatus.FORBIDDEN, "sin-permiso", "Sin permiso",
            "No tienes permiso para realizar esta acción."),
    CAMBIO_PASSWORD_REQUERIDO(HttpStatus.FORBIDDEN, "cambio-password-requerido", "Cambio de contraseña requerido",
            "Debes cambiar tu contraseña antes de continuar."),
    NO_ENCONTRADO(HttpStatus.NOT_FOUND, "no-encontrado", "Recurso no encontrado",
            "El recurso solicitado no existe."),
    METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED, "metodo-no-permitido", "Método no permitido",
            "Este recurso no admite el método usado."),
    CONFLICTO(HttpStatus.CONFLICT, "conflicto", "Conflicto",
            "La operación entra en conflicto con el estado actual del recurso."),
    VERSION_DESACTUALIZADA(HttpStatus.CONFLICT, "version-desactualizada", "Registro modificado",
            "Otra persona modificó este registro. Recarga los datos e inténtalo de nuevo."),
    ARCHIVO_DEMASIADO_GRANDE(HttpStatus.CONTENT_TOO_LARGE, "archivo-demasiado-grande", "Archivo demasiado grande",
            "El archivo supera el máximo de 10 MB."),
    TIPO_NO_SOPORTADO(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "tipo-no-soportado", "Tipo no soportado",
            "El tipo de contenido no está permitido."),
    DEMASIADOS_INTENTOS(HttpStatus.TOO_MANY_REQUESTS, "demasiados-intentos", "Demasiados intentos",
            "Has hecho demasiados intentos. Espera un minuto e inténtalo de nuevo."),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
            "Ocurrió un error inesperado. Inténtalo de nuevo más tarde.");

    private static final String BASE = "https://adminpro.dev/errores/";

    private final HttpStatus status;
    private final URI type;
    private final String title;
    private final String detalle;

    TipoError(HttpStatus status, String slug, String title, String detalle) {
        this.status = status;
        this.type = URI.create(BASE + slug);
        this.title = title;
        this.detalle = detalle;
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String detalle() {
        return detalle;
    }

    /** Traduce los errores propios de Spring MVC, que solo traen un código HTTP. */
    public static TipoError desdeStatus(int status) {
        return switch (status) {
            case 401 -> NO_AUTENTICADO;
            case 403 -> SIN_PERMISO;
            case 404 -> NO_ENCONTRADO;
            case 405 -> METODO_NO_PERMITIDO;
            case 409 -> CONFLICTO;
            case 413 -> ARCHIVO_DEMASIADO_GRANDE;
            case 415 -> TIPO_NO_SOPORTADO;
            case 429 -> DEMASIADOS_INTENTOS;
            default -> status >= 500 ? ERROR_INTERNO : SOLICITUD_INVALIDA;
        };
    }
}
