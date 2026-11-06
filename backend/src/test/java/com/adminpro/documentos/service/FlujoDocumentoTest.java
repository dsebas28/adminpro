package com.adminpro.documentos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.adminpro.documentos.entity.EstadoDocumento;
import com.adminpro.documentos.service.FlujoDocumento.Accion;

class FlujoDocumentoTest {

    private static final Set<String> SUPERVISOR = Set.of("DOCUMENTOS_ESCRIBIR", "DOCUMENTOS_APROBAR");
    private static final Set<String> AUXILIAR = Set.of("DOCUMENTOS_ESCRIBIR");

    @ParameterizedTest
    @CsvSource({
            "ENVIAR,REGISTRADO,PENDIENTE",
            "INICIAR_REVISION,PENDIENTE,EN_REVISION",
            "APROBAR,EN_REVISION,APROBADO",
            "DEVOLVER,EN_REVISION,PENDIENTE",
            "FINALIZAR,APROBADO,FINALIZADO"
    })
    void cadaAccionTieneSuOrigenYDestino(Accion accion, EstadoDocumento origen, EstadoDocumento destino) {
        assertThat(accion.aplicaDesde(origen)).isTrue();
        assertThat(accion.destino()).isEqualTo(destino);
    }

    @Test
    void ningunaAccionSaleDeFinalizado() {
        for (Accion accion : Accion.values()) {
            assertThat(accion.aplicaDesde(EstadoDocumento.FINALIZADO)).isFalse();
        }
    }

    @Test
    void unAuxiliarEnviaPeroNoRevisa() {
        assertThat(acciones(EstadoDocumento.REGISTRADO, true, false, AUXILIAR)).containsExactly("EDITAR", "ENVIAR");
        assertThat(acciones(EstadoDocumento.PENDIENTE, true, false, AUXILIAR)).containsExactly("EDITAR");
        assertThat(acciones(EstadoDocumento.EN_REVISION, true, false, AUXILIAR)).isEmpty();
    }

    @Test
    void elSupervisorApruebaOdevuelve() {
        assertThat(acciones(EstadoDocumento.EN_REVISION, true, false, SUPERVISOR))
                .containsExactly("APROBAR", "DEVOLVER");
    }

    @Test
    void elCreadorNoVeAprobarPeroSiDevolver() {
        assertThat(acciones(EstadoDocumento.EN_REVISION, true, true, SUPERVISOR)).containsExactly("DEVOLVER");
    }

    @Test
    void unDocumentoInactivoNoTieneAcciones() {
        assertThat(acciones(EstadoDocumento.REGISTRADO, false, false, SUPERVISOR)).isEmpty();
    }

    @Test
    void consultaNoTieneAcciones() {
        assertThat(acciones(EstadoDocumento.REGISTRADO, true, false, Set.of("DOCUMENTOS_LEER"))).isEmpty();
    }

    private java.util.List<String> acciones(EstadoDocumento estado, boolean activo, boolean creador, Set<String> permisos) {
        return FlujoDocumento.accionesDisponibles(estado, activo, creador, permisos::contains);
    }
}
