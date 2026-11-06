package com.adminpro.empleados.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.common.catalogo.CatalogoController;
import com.adminpro.empleados.entity.Departamento;
import com.adminpro.empleados.service.DepartamentoService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/departamentos")
@Tag(name = "Departamentos")
public class DepartamentoController extends CatalogoController<Departamento> {

    public DepartamentoController(DepartamentoService servicio) {
        super(servicio);
    }

    @Override
    public String permisoLectura() {
        return "EMPLEADOS_LEER";
    }
}
