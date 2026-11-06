package com.adminpro.usuarios.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import com.adminpro.common.BaseEntity;

/** Catálogo fijo: se carga por migración y la aplicación solo lo lee. */
@Entity
@Immutable
@Table(name = "roles")
public class Rol extends BaseEntity {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    private String descripcion;

    @ManyToMany
    @JoinTable(name = "rol_permisos",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new HashSet<>();

    protected Rol() {
    }

    public List<String> codigosPermisos() {
        return permisos.stream().map(Permiso::getCodigo).sorted().toList();
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }
}
