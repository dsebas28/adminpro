package com.adminpro.usuarios.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de un empleado, lo único que el módulo de usuarios necesita
 * saber de él. Evita acoplar este módulo a la entidad completa de empleados.
 */
@Entity
@Immutable
@Table(name = "empleados")
public class EmpleadoRef {

    @Id
    private Long id;

    @Formula("nombres || ' ' || apellidos")
    private String nombre;

    private boolean activo;

    protected EmpleadoRef() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }
}
