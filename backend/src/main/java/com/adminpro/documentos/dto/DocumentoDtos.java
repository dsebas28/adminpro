package com.adminpro.documentos.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.adminpro.common.RefResponse;
import com.adminpro.documentos.entity.AccionHistorial;
import com.adminpro.documentos.entity.EstadoDocumento;

/** Los DTOs del módulo de documentos, agrupados porque son registros pequeños y relacionados. */
public final class DocumentoDtos {

    private DocumentoDtos() {
    }

    public record DocumentoRequest(
            @NotBlank(message = "El título es obligatorio")
            @Size(max = 200, message = "Máximo 200 caracteres")
            String titulo,

            @Size(max = 2000, message = "Máximo 2000 caracteres")
            String descripcion,

            @NotNull(message = "La categoría es obligatoria")
            Long categoriaId,

            @NotNull(message = "El responsable es obligatorio")
            Long responsableId,

            @NotNull(message = "La fecha del documento es obligatoria")
            LocalDate fechaDocumento,

            LocalDate fechaVencimiento,

            Long version) {
    }

    public record TransicionRequest(
            @NotNull(message = "La versión es obligatoria")
            Long version,

            @Size(max = 1000, message = "Máximo 1000 caracteres")
            String comentario) {
    }

    public record DocumentoListItemResponse(
            Long id,
            String consecutivo,
            String titulo,
            RefResponse categoria,
            RefResponse responsable,
            EstadoDocumento estado,
            LocalDate fechaDocumento,
            LocalDate fechaVencimiento,
            boolean vencido,
            boolean activo) {
    }

    public record AdjuntoResponse(Long id, String nombreOriginal, String tipoMime, long tamanoBytes,
            Instant createdAt) {
    }

    public record DocumentoResponse(
            Long id,
            String consecutivo,
            String titulo,
            String descripcion,
            RefResponse categoria,
            RefResponse responsable,
            EstadoDocumento estado,
            LocalDate fechaDocumento,
            LocalDate fechaVencimiento,
            boolean vencido,
            boolean activo,
            List<AdjuntoResponse> adjuntos,
            List<String> accionesDisponibles,
            long version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record HistorialDocumentoResponse(
            Long id,
            AccionHistorial accion,
            EstadoDocumento estadoAnterior,
            EstadoDocumento estadoNuevo,
            String comentario,
            RefResponse usuario,
            Instant createdAt) {
    }
}
