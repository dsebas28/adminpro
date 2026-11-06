package com.adminpro.usuarios.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.adminpro.usuarios.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = {"rol.permisos", "empleado"})
    @Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);

    @EntityGraph(attributePaths = {"rol.permisos", "empleado"})
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarCompleto(@Param("id") Long id);

    /** Serializa los intentos simultáneos para que ningún fallo se pierda en el conteo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaActualizar(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"rol", "empleado"})
    Page<Usuario> findAll(Specification<Usuario> filtro, Pageable paginacion);

    @Query("select count(u) > 0 from Usuario u where lower(u.correo) = lower(:correo)")
    boolean existeCorreo(@Param("correo") String correo);

    boolean existsByEmpleado_IdAndIdNot(Long empleadoId, Long usuarioId);

    boolean existsByEmpleado_Id(Long empleadoId);

    long countByRol_CodigoAndActivoTrue(String codigoRol);
}
