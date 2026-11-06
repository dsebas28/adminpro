package com.adminpro.auth.dto;

import java.util.List;

import com.adminpro.usuarios.entity.Usuario;

public record UsuarioActualResponse(
        Long id,
        String correo,
        String nombreVisible,
        String rol,
        List<String> permisos,
        boolean debeCambiarPassword) {

    public static UsuarioActualResponse de(Usuario usuario) {
        return new UsuarioActualResponse(usuario.getId(), usuario.getCorreo(), usuario.getNombreVisible(),
                usuario.getRol().getCodigo(), usuario.getRol().codigosPermisos(), usuario.isDebeCambiarPassword());
    }
}
