package com.adminpro.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validacionDevuelveErroresPorCampo() throws Exception {
        mvc.perform(post("/prueba").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"12A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDACION"))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.instance").value("/prueba"))
                .andExpect(jsonPath("$.errors[0].field").value("cedula"))
                .andExpect(jsonPath("$.errors[0].message").value("Debe tener entre 6 y 10 dígitos"));
    }

    @Test
    void jsonMalFormadoEsSolicitudInvalida() throws Exception {
        mvc.perform(post("/prueba").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void noEncontradoConservaElMensajeDeNegocio() throws Exception {
        mvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.detail").value("El empleado no existe"));
    }

    @Test
    void conflictoDeNegocioEs409() throws Exception {
        mvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO"));
    }

    @Test
    void versionDesactualizadaEs409() throws Exception {
        mvc.perform(get("/prueba/version"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_DESACTUALIZADA"));
    }

    @Test
    void integridadNoFiltraDetallesDeLaBaseDeDatos() throws Exception {
        mvc.perform(get("/prueba/integridad"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO"))
                .andExpect(jsonPath("$.detail").value(TipoError.CONFLICTO.detalle()));
    }

    @Test
    void errorInesperadoNoFiltraElMensajeInterno() throws Exception {
        mvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.detail").value(TipoError.ERROR_INTERNO.detalle()));
    }

    @Test
    void metodoNoPermitidoEnEspanol() throws Exception {
        mvc.perform(post("/prueba/conflicto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METODO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.title").value("Método no permitido"));
    }

    record Solicitud(
            @NotBlank @Pattern(regexp = "^[0-9]{6,10}$", message = "Debe tener entre 6 y 10 dígitos") String cedula) {
    }

    @RestController
    static class ControladorDePrueba {

        @PostMapping("/prueba")
        void crear(@Valid @RequestBody Solicitud solicitud) {
        }

        @GetMapping("/prueba/no-encontrado")
        void noEncontrado() {
            throw ApiException.noEncontrado("El empleado no existe");
        }

        @GetMapping("/prueba/conflicto")
        void conflicto() {
            throw ApiException.conflicto("La cédula ya está registrada");
        }

        @GetMapping("/prueba/version")
        void version() {
            throw new OptimisticLockingFailureException("fila cambiada");
        }

        @GetMapping("/prueba/integridad")
        void integridad() {
            throw new DataIntegrityViolationException("uq_empleados_cedula");
        }

        @GetMapping("/prueba/inesperado")
        void inesperado() {
            throw new IllegalStateException("contraseña del servidor: 1234");
        }
    }
}
