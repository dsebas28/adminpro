package com.adminpro.exception;

import java.net.URI;
import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte toda excepción en Problem Details (RFC 9457) con mensajes en español.
 * Hereda de ResponseEntityExceptionHandler para cubrir también los errores propios
 * de Spring MVC, que de otro modo llegarían al cliente en inglés.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> manejarApi(ApiException ex, WebRequest request) {
        return responder(ex, ex.tipo(), ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> manejarRestricciones(ConstraintViolationException ex, WebRequest request) {
        List<ErrorCampo> errores = ex.getConstraintViolations().stream()
                .map(v -> new ErrorCampo(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(ex, TipoError.VALIDACION, TipoError.VALIDACION.detalle(), errores, request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Object> manejarVersion(OptimisticLockingFailureException ex, WebRequest request) {
        TipoError tipo = TipoError.VERSION_DESACTUALIZADA;
        return responder(ex, tipo, tipo.detalle(), List.of(), request);
    }

    /** Red de seguridad: los servicios deben validar antes y dar un mensaje más específico. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> manejarIntegridad(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Violación de integridad no controlada: {}", ex.getMostSpecificCause().getMessage());
        TipoError tipo = TipoError.CONFLICTO;
        return responder(ex, tipo, tipo.detalle(), List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> manejarInesperado(Exception ex, WebRequest request) {
        log.error("Error no controlado", ex);
        TipoError tipo = TipoError.ERROR_INTERNO;
        return responder(ex, tipo, tipo.detalle(), List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(ex, TipoError.VALIDACION, TipoError.VALIDACION.detalle(), errores, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErrorCampo(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return responder(ex, TipoError.VALIDACION, TipoError.VALIDACION.detalle(), errores, request);
    }

    /** Todo error de Spring MVC pasa por aquí: se reemplaza su cuerpo por el formato propio. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail detalle && detalle.getProperties() != null
                && detalle.getProperties().containsKey("code")) {
            return super.handleExceptionInternal(ex, body, headers, statusCode, request);
        }
        TipoError tipo = TipoError.desdeStatus(statusCode.value());
        ProblemDetail problema = crearProblema(tipo, statusCode, tipo.detalle(), List.of(), request);
        return super.handleExceptionInternal(ex, problema, headers, statusCode, request);
    }

    private ResponseEntity<Object> responder(
            Exception ex, TipoError tipo, String detalle, List<ErrorCampo> errores, WebRequest request) {
        ProblemDetail problema = crearProblema(tipo, tipo.status(), detalle, errores, request);
        return super.handleExceptionInternal(ex, problema, new HttpHeaders(), tipo.status(), request);
    }

    private ProblemDetail crearProblema(
            TipoError tipo, HttpStatusCode status, String detalle, List<ErrorCampo> errores, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setType(tipo.type());
        problema.setTitle(tipo.title());
        if (request instanceof ServletWebRequest servlet) {
            problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
        }
        problema.setProperty("code", tipo.name());
        if (!errores.isEmpty()) {
            problema.setProperty("errors", errores);
        }
        return problema;
    }
}
