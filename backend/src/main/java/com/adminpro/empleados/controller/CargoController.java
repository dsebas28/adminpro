package com.adminpro.empleados.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adminpro.common.catalogo.CatalogoController;
import com.adminpro.empleados.entity.Cargo;
import com.adminpro.empleados.service.CargoService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/cargos")
@Tag(name = "Cargos")
public class CargoController extends CatalogoController<Cargo> {

    public CargoController(CargoService servicio) {
        super(servicio);
    }

    @Override
    public String permisoLectura() {
        return "EMPLEADOS_LEER";
    }
}
