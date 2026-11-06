package com.adminpro.usuarios.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.adminpro.common.RefResponse;
import com.adminpro.usuarios.dto.RolResponse;
import com.adminpro.usuarios.dto.UsuarioResponse;
import com.adminpro.usuarios.entity.EmpleadoRef;
import com.adminpro.usuarios.entity.Rol;
import com.adminpro.usuarios.entity.Usuario;

@Mapper
public interface UsuarioMapper {

    UsuarioResponse aRespuesta(Usuario usuario);

    @Mapping(target = "permisos", expression = "java(rol.codigosPermisos())")
    RolResponse aRespuesta(Rol rol);

    default RefResponse referencia(Rol rol) {
        return rol == null ? null : new RefResponse(rol.getId(), rol.getNombre());
    }

    default RefResponse referencia(EmpleadoRef empleado) {
        return empleado == null ? null : new RefResponse(empleado.getId(), empleado.getNombre());
    }
}
