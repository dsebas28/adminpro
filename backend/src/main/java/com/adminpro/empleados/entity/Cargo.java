package com.adminpro.empleados.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.adminpro.common.catalogo.CatalogoEntity;

@Entity
@Table(name = "cargos")
public class Cargo extends CatalogoEntity {

    protected Cargo() {
    }

    public Cargo(String nombre, String descripcion) {
        super(nombre, descripcion);
    }
}
