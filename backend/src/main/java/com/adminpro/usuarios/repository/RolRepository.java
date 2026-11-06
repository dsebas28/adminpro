package com.adminpro.usuarios.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.adminpro.usuarios.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {

    @EntityGraph(attributePaths = "permisos")
    @Query("select r from Rol r order by r.id")
    List<Rol> listarConPermisos();

    Optional<Rol> findByCodigo(String codigo);
}
