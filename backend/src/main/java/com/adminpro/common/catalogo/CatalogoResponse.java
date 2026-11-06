package com.adminpro.common.catalogo;

public record CatalogoResponse(Long id, String nombre, String descripcion, boolean activo, long version) {

    public static CatalogoResponse de(CatalogoEntity entidad) {
        return new CatalogoResponse(entidad.getId(), entidad.getNombre(), entidad.getDescripcion(),
                entidad.isActivo(), entidad.getVersion());
    }
}
