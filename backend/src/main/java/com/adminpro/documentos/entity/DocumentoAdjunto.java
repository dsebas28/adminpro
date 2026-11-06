package com.adminpro.documentos.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.adminpro.common.BaseEntity;

@Entity
@Table(name = "documento_adjuntos")
public class DocumentoAdjunto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento_id", nullable = false, updatable = false)
    private Long documentoId;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private UUID storageKey;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    private boolean activo = true;

    protected DocumentoAdjunto() {
    }

    public DocumentoAdjunto(Long documentoId, String nombreOriginal, UUID storageKey, String tipoMime,
            long tamanoBytes, String sha256) {
        this.documentoId = documentoId;
        this.nombreOriginal = nombreOriginal;
        this.storageKey = storageKey;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.sha256 = sha256;
    }

    public void desactivar() {
        activo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public UUID getStorageKey() {
        return storageKey;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public boolean isActivo() {
        return activo;
    }
}
