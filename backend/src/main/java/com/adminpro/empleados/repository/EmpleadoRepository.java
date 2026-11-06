package com.adminpro.empleados.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.adminpro.empleados.entity.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long>, JpaSpecificationExecutor<Empleado> {

    @Override
    @EntityGraph(attributePaths = {"departamento", "cargo"})
    Page<Empleado> findAll(Specification<Empleado> filtro, Pageable paginacion);

    @EntityGraph(attributePaths = {"departamento", "cargo"})
    @Query("select e from Empleado e where e.id = :id")
    Optional<Empleado> buscarCompleto(@Param("id") Long id);

    boolean existsByCedula(String cedula);

    boolean existsByCedulaAndIdNot(String cedula, Long id);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByDepartamento_IdAndActivoTrue(Long departamentoId);

    boolean existsByCargo_IdAndActivoTrue(Long cargoId);

    long countByActivo(boolean activo);
}
