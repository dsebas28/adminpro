package com.adminpro.common;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.OptimisticLock;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Columnas de auditoría que llevan todas las tablas. */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Excluidas del control de versión: son consecuencia de un cambio, no datos de negocio.
    // Sin esto, tocar un campo excluido (como el último acceso) subiría la versión igual.
    @OptimisticLock(excluded = true)
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @OptimisticLock(excluded = true)
    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    /** Para eventos sin sesión, como un login: el autor no se puede leer del contexto de seguridad. */
    protected void asignarAutor(Long usuarioId) {
        this.createdBy = usuarioId;
        this.updatedBy = usuarioId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
