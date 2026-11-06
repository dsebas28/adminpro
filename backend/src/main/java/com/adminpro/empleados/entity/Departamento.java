package com.adminpro.empleados.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.adminpro.common.catalogo.CatalogoEntity;

@Entity
@Table(name = "departamentos")
public class Departamento extends CatalogoEntity {

    protected Departamento() {
    }

    public Departamento(String nombre, String descripcion) {
        super(nombre, descripcion);
    }
}
