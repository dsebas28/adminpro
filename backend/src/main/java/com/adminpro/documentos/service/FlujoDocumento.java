package com.adminpro.documentos.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.adminpro.documentos.entity.EstadoDocumento;

/** Las transiciones válidas del documento, en un solo lugar y sin dependencias para probarlas fácil. */
public final class FlujoDocumento {

    public enum Accion {
        ENVIAR(EstadoDocumento.REGISTRADO, EstadoDocumento.PENDIENTE, "DOCUMENTOS_ESCRIBIR"),
        INICIAR_REVISION(EstadoDocumento.PENDIENTE, EstadoDocumento.EN_REVISION, "DOCUMENTOS_APROBAR"),
        APROBAR(EstadoDocumento.EN_REVISION, EstadoDocumento.APROBADO, "DOCUMENTOS_APROBAR"),
        DEVOLVER(EstadoDocumento.EN_REVISION, EstadoDocumento.PENDIENTE, "DOCUMENTOS_APROBAR"),
        FINALIZAR(EstadoDocumento.APROBADO, EstadoDocumento.FINALIZADO, "DOCUMENTOS_APROBAR");

        private final EstadoDocumento origen;
        private final EstadoDocumento destino;
        private final String permiso;

        Accion(EstadoDocumento origen, EstadoDocumento destino, String permiso) {
            this.origen = origen;
            this.destino = destino;
            this.permiso = permiso;
        }

        public EstadoDocumento destino() {
            return destino;
        }

        public boolean aplicaDesde(EstadoDocumento estado) {
            return origen == estado;
        }
    }

    private FlujoDocumento() {
    }

    /**
     * Lo que este usuario puede hacer ahora con el documento. El frontend lo usa para mostrar
     * solo los botones válidos; aun así, el backend vuelve a validar cada acción.
     */
    public static List<String> accionesDisponibles(EstadoDocumento estado, boolean activo, boolean esCreador,
            Predicate<String> tienePermiso) {
        if (!activo) {
            return List.of();
        }
        List<String> acciones = new ArrayList<>();
        if (estado.esEditable() && tienePermiso.test("DOCUMENTOS_ESCRIBIR")) {
            acciones.add("EDITAR");
        }
        Arrays.stream(Accion.values())
                .filter(accion -> accion.aplicaDesde(estado) && tienePermiso.test(accion.permiso))
                .filter(accion -> accion != Accion.APROBAR || !esCreador)
                .map(Accion::name)
                .forEach(acciones::add);
        return acciones;
    }
}
