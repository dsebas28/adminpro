package com.adminpro.documentos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import com.adminpro.common.BaseEntity;

/** Línea de tiempo del documento. Solo inserción: la base de datos rechaza UPDATE y DELETE. */
@Entity
@Immutable
@Table(name = "documento_historial")
public class DocumentoHistorial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionHistorial accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoDocumento estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", length = 20)
    private EstadoDocumento estadoNuevo;

    private String comentario;

    protected DocumentoHistorial() {
    }

    public DocumentoHistorial(Long documentoId, AccionHistorial accion, EstadoDocumento estadoAnterior,
            EstadoDocumento estadoNuevo, String comentario) {
        this.documentoId = documentoId;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.comentario = comentario;
    }

    public Long getId() {
        return id;
    }

    public AccionHistorial getAccion() {
        return accion;
    }

    public EstadoDocumento getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoDocumento getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getComentario() {
        return comentario;
    }
}
