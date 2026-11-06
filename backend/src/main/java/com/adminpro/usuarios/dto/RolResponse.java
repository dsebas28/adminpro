package com.adminpro.usuarios.dto;

import java.util.List;

public record RolResponse(Long id, String codigo, String nombre, List<String> permisos) {
}
