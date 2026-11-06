package com.adminpro.empleados.mapper;

import org.mapstruct.Mapper;

import com.adminpro.common.RefResponse;
import com.adminpro.common.catalogo.CatalogoEntity;
import com.adminpro.empleados.dto.EmpleadoResponse;
import com.adminpro.empleados.entity.Empleado;

@Mapper
public interface EmpleadoMapper {

    EmpleadoResponse aRespuesta(Empleado empleado);

    default RefResponse referencia(CatalogoEntity catalogo) {
        return catalogo == null ? null : new RefResponse(catalogo.getId(), catalogo.getNombre());
    }
}
