package com.adminpro.documentos.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.common.catalogo.CatalogoController;
import com.adminpro.documentos.entity.CategoriaDocumento;
import com.adminpro.documentos.service.CategoriaDocumentoService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/categorias-documento")
@Tag(name = "Categorías de documento")
public class CategoriaDocumentoController extends CatalogoController<CategoriaDocumento> {

    public CategoriaDocumentoController(CategoriaDocumentoService servicio) {
        super(servicio);
    }

    @Override
    public String permisoLectura() {
        return "DOCUMENTOS_LEER";
    }
}
