package com.adminpro.documentos.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.adminpro.common.BaseEntity;
import com.adminpro.empleados.entity.Empleado;

@Entity
@Table(name = "documentos")
public class Documento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 15)
    private String consecutivo;

    @Column(nullable = false, length = 200)
    private String titulo;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private CategoriaDocumento categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id")
    private Empleado responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.REGISTRADO;

    @Column(name = "fecha_documento", nullable = false)
    private LocalDate fechaDocumento;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    private long version;

    protected Documento() {
    }

    public Documento(String consecutivo, String titulo, String descripcion, CategoriaDocumento categoria,
            Empleado responsable, LocalDate fechaDocumento, LocalDate fechaVencimiento) {
        this.consecutivo = consecutivo;
        actualizar(titulo, descripcion, categoria, responsable, fechaDocumento, fechaVencimiento);
    }

    public final void actualizar(String titulo, String descripcion, CategoriaDocumento categoria,
            Empleado responsable, LocalDate fechaDocumento, LocalDate fechaVencimiento) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.responsable = responsable;
        this.fechaDocumento = fechaDocumento;
        this.fechaVencimiento = fechaVencimiento;
    }

    public void cambiarEstado(EstadoDocumento nuevo) {
        estado = nuevo;
    }

    public void activar() {
        activo = true;
    }

    public void desactivar() {
        activo = false;
    }

    /** Un documento finalizado ya cumplió su ciclo: su vencimiento deja de generar alertas. */
    public boolean estaVencido(LocalDate hoy) {
        return fechaVencimiento != null && fechaVencimiento.isBefore(hoy) && estado != EstadoDocumento.FINALIZADO;
    }

    public Long getId() {
        return id;
    }

    public String getConsecutivo() {
        return consecutivo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public CategoriaDocumento getCategoria() {
        return categoria;
    }

    public Empleado getResponsable() {
        return responsable;
    }

    public EstadoDocumento getEstado() {
        return estado;
    }

    public LocalDate getFechaDocumento() {
        return fechaDocumento;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public long getVersion() {
        return version;
    }
}
