package com.adminpro.documentos.service;

import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.adminpro.audit.entity.AccionAuditoria;
import com.adminpro.audit.service.AuditService;
import com.adminpro.documentos.dto.DocumentoDtos.AdjuntoResponse;
import com.adminpro.documentos.entity.AccionHistorial;
import com.adminpro.documentos.entity.Documento;
import com.adminpro.documentos.entity.DocumentoAdjunto;
import com.adminpro.documentos.entity.DocumentoHistorial;
import com.adminpro.documentos.repository.DocumentoAdjuntoRepository;
import com.adminpro.documentos.repository.DocumentoHistorialRepository;
import com.adminpro.documentos.repository.DocumentoRepository;
import com.adminpro.exception.ApiException;
import com.adminpro.storage.StorageService;
import com.adminpro.storage.TipoArchivoValidator;

@Service
@Transactional(readOnly = true)
public class AdjuntoService {

    private static final int MAX_ADJUNTOS = 10;
    private static final int MAX_NOMBRE = 255;
    private static final String ENTIDAD = "DOCUMENTO";

    public record Descarga(Resource archivo, String nombre, String tipoMime) {
    }

    private final DocumentoRepository documentos;
    private final DocumentoAdjuntoRepository adjuntos;
    private final DocumentoHistorialRepository historial;
    private final StorageService almacenamiento;
    private final TipoArchivoValidator validador;
    private final AuditService auditoria;

    public AdjuntoService(DocumentoRepository documentos, DocumentoAdjuntoRepository adjuntos,
            DocumentoHistorialRepository historial, StorageService almacenamiento,
            TipoArchivoValidator validador, AuditService auditoria) {
        this.documentos = documentos;
        this.adjuntos = adjuntos;
        this.historial = historial;
        this.almacenamiento = almacenamiento;
        this.validador = validador;
        this.auditoria = auditoria;
    }

    public List<AdjuntoResponse> listar(Long documentoId) {
        buscarDocumento(documentoId);
        return adjuntos.findByDocumentoIdAndActivoTrueOrderById(documentoId).stream()
                .map(DocumentoService::aAdjunto).toList();
    }

    @Transactional
    public AdjuntoResponse agregar(Long documentoId, String nombreOriginal, byte[] contenido) {
        Documento documento = buscarDocumento(documentoId);
        exigirEditable(documento);
        if (contenido.length == 0) {
            throw ApiException.campo("archivo", "El archivo está vacío");
        }
        if (adjuntos.countByDocumentoIdAndActivoTrue(documentoId) >= MAX_ADJUNTOS) {
            throw ApiException.conflicto("El documento ya tiene el máximo de 10 adjuntos");
        }
        String tipo = validador.tipoReal(contenido);
        String nombre = nombreSeguro(nombreOriginal);
        UUID clave = UUID.randomUUID();

        almacenamiento.guardar(clave, contenido);
        borrarSiSeRevierte(clave);

        DocumentoAdjunto adjunto = adjuntos.save(new DocumentoAdjunto(documentoId, nombre, clave, tipo,
                contenido.length, sha256(contenido)));
        historial.save(new DocumentoHistorial(documentoId, AccionHistorial.ADJUNTO_AGREGADO, null, null, nombre));
        auditoria.registrar(ENTIDAD, documentoId, AccionAuditoria.ADJUNTO_AGREGADO, null, foto(adjunto));
        return DocumentoService.aAdjunto(adjunto);
    }

    public Descarga descargar(Long documentoId, Long adjuntoId) {
        DocumentoAdjunto adjunto = buscarAdjunto(documentoId, adjuntoId);
        Resource archivo = almacenamiento.abrir(adjunto.getStorageKey())
                .orElseThrow(() -> ApiException.noEncontrado("El archivo no está disponible"));
        return new Descarga(archivo, adjunto.getNombreOriginal(), adjunto.getTipoMime());
    }

    /** Borrado lógico: el archivo se conserva porque forma parte del historial del documento. */
    @Transactional
    public void eliminar(Long documentoId, Long adjuntoId) {
        exigirEditable(buscarDocumento(documentoId));
        DocumentoAdjunto adjunto = buscarAdjunto(documentoId, adjuntoId);
        adjunto.desactivar();
        historial.save(new DocumentoHistorial(documentoId, AccionHistorial.ADJUNTO_ELIMINADO, null, null,
                adjunto.getNombreOriginal()));
        auditoria.registrar(ENTIDAD, documentoId, AccionAuditoria.ADJUNTO_ELIMINADO, foto(adjunto), null);
    }

    /** El archivo se escribe antes del commit: si la transacción falla, no debe quedar huérfano. */
    private void borrarSiSeRevierte(UUID clave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    almacenamiento.eliminar(clave);
                }
            }
        });
    }

    private void exigirEditable(Documento documento) {
        if (!documento.isActivo() || !documento.getEstado().esEditable()) {
            throw ApiException.conflicto("Los adjuntos ya no se pueden cambiar en el estado actual del documento");
        }
    }

    private Documento buscarDocumento(Long id) {
        return documentos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El documento no existe"));
    }

    private DocumentoAdjunto buscarAdjunto(Long documentoId, Long adjuntoId) {
        return adjuntos.findByIdAndDocumentoIdAndActivoTrue(adjuntoId, documentoId)
                .orElseThrow(() -> ApiException.noEncontrado("El adjunto no existe"));
    }

    /** Quita las rutas que pueda traer el nombre y lo recorta a lo que admite la columna. */
    private String nombreSeguro(String original) {
        String nombre = original == null ? "" : original.replace('\\', '/');
        nombre = nombre.isBlank() ? "" : String.valueOf(Paths.get(nombre.replace("\0", "")).getFileName());
        if (nombre.isBlank() || "null".equals(nombre)) {
            return "archivo";
        }
        return nombre.length() <= MAX_NOMBRE ? nombre : nombre.substring(nombre.length() - MAX_NOMBRE);
    }

    private String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private Map<String, Object> foto(DocumentoAdjunto adjunto) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("nombre", adjunto.getNombreOriginal());
        campos.put("tipo", adjunto.getTipoMime());
        campos.put("tamanoBytes", adjunto.getTamanoBytes());
        return campos;
    }
}
