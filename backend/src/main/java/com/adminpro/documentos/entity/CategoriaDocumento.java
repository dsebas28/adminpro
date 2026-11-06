package com.adminpro.documentos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.adminpro.common.catalogo.CatalogoEntity;

@Entity
@Table(name = "categorias_documento")
public class CategoriaDocumento extends CatalogoEntity {

    protected CategoriaDocumento() {
    }

    public CategoriaDocumento(String nombre, String descripcion) {
        super(nombre, descripcion);
    }
}
