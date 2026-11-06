package com.adminpro.empleados.service;

import org.springframework.stereotype.Service;

import com.adminpro.audit.service.AuditService;
import com.adminpro.common.catalogo.CatalogoService;
import com.adminpro.empleados.entity.Cargo;
import com.adminpro.empleados.repository.CargoRepository;
import com.adminpro.empleados.repository.EmpleadoRepository;

@Service
public class CargoService extends CatalogoService<Cargo> {

    private final EmpleadoRepository empleados;

    public CargoService(CargoRepository repositorio, EmpleadoRepository empleados, AuditService auditoria) {
        super(repositorio, auditoria, "CARGO", "cargo");
        this.empleados = empleados;
    }

    @Override
    protected Cargo nueva(String nombre, String descripcion) {
        return new Cargo(nombre, descripcion);
    }

    @Override
    protected boolean enUso(Long id) {
        return empleados.existsByCargo_IdAndActivoTrue(id);
    }
}
