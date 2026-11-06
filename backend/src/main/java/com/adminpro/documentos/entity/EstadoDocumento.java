package com.adminpro.documentos.entity;

public enum EstadoDocumento {
    REGISTRADO,
    PENDIENTE,
    EN_REVISION,
    APROBADO,
    FINALIZADO;

    /** Desde En revisión los datos y los adjuntos quedan congelados (supuesto S12). */
    public boolean esEditable() {
        return this == REGISTRADO || this == PENDIENTE;
    }
}
