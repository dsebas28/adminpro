package com.adminpro.empleados.service;

import org.springframework.stereotype.Service;

import com.adminpro.audit.service.AuditService;
import com.adminpro.common.catalogo.CatalogoService;
import com.adminpro.empleados.entity.Departamento;
import com.adminpro.empleados.repository.DepartamentoRepository;
import com.adminpro.empleados.repository.EmpleadoRepository;

@Service
public class DepartamentoService extends CatalogoService<Departamento> {

    private final EmpleadoRepository empleados;

    public DepartamentoService(DepartamentoRepository repositorio, EmpleadoRepository empleados,
            AuditService auditoria) {
        super(repositorio, auditoria, "DEPARTAMENTO", "departamento");
        this.empleados = empleados;
    }

    @Override
    protected Departamento nueva(String nombre, String descripcion) {
        return new Departamento(nombre, descripcion);
    }

    @Override
    protected boolean enUso(Long id) {
        return empleados.existsByDepartamento_IdAndActivoTrue(id);
    }
}
