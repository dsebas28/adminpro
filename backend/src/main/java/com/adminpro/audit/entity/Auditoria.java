package com.adminpro.audit.entity;

import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.adminpro.common.BaseEntity;

/** Solo inserción: la base de datos rechaza UPDATE y DELETE sobre esta tabla. */
@Entity
@Immutable
@Table(name = "auditoria")
public class Auditoria extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionAuditoria accion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior", columnDefinition = "jsonb")
    private Map<String, Object> valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo", columnDefinition = "jsonb")
    private Map<String, Object> valorNuevo;

    @Column(length = 45)
    private String ip;

    protected Auditoria() {
    }

    public Auditoria(String entidad, Long entidadId, AccionAuditoria accion, Map<String, Object> valorAnterior,
            Map<String, Object> valorNuevo, String ip, Long autorExplicito) {
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.ip = ip;
        if (autorExplicito != null) {
            asignarAutor(autorExplicito);
        }
    }

    public Long getId() {
        return id;
    }

    public String getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public Map<String, Object> getValorAnterior() {
        return valorAnterior;
    }

    public Map<String, Object> getValorNuevo() {
        return valorNuevo;
    }

    public String getIp() {
        return ip;
    }
}
