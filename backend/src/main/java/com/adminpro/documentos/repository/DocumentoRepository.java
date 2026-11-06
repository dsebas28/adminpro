package com.adminpro.documentos.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.adminpro.documentos.entity.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    /** Conteo de documentos activos por estado. */
    interface ConteoEstado {
        com.adminpro.documentos.entity.EstadoDocumento getEstado();

        long getTotal();
    }

    interface ReferenciaConsecutivo {
        Long getId();

        String getConsecutivo();
    }

    @Override
    @EntityGraph(attributePaths = {"categoria", "responsable"})
    Page<Documento> findAll(Specification<Documento> filtro, Pageable paginacion);

    @EntityGraph(attributePaths = {"categoria", "responsable"})
    @Query("select d from Documento d where d.id = :id")
    Optional<Documento> buscarCompleto(@Param("id") Long id);

    boolean existsByCategoria_IdAndActivoTrue(Long categoriaId);

    long countByActivoTrue();

    @Query("select d.estado as estado, count(d) as total from Documento d where d.activo = true group by d.estado")
    List<ConteoEstado> contarPorEstado();

    @Query("""
            select count(d) from Documento d
            where d.activo = true and d.estado <> com.adminpro.documentos.entity.EstadoDocumento.FINALIZADO
              and d.fechaVencimiento < :hoy
            """)
    long contarVencidos(@Param("hoy") LocalDate hoy);

    @Query("""
            select count(d) from Documento d
            where d.activo = true and d.estado <> com.adminpro.documentos.entity.EstadoDocumento.FINALIZADO
              and d.fechaVencimiento between :desde and :hasta
            """)
    long contarPorVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @EntityGraph(attributePaths = {"categoria", "responsable"})
    @Query("""
            select d from Documento d
            where d.activo = true and d.estado <> com.adminpro.documentos.entity.EstadoDocumento.FINALIZADO
              and d.fechaVencimiento between :desde and :hasta
            """)
    Page<Documento> porVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, Pageable paginacion);

    @Query("""
            select d from Documento d
            where d.activo = true and d.estado <> com.adminpro.documentos.entity.EstadoDocumento.FINALIZADO
              and d.fechaVencimiento < :hoy
            order by d.fechaVencimiento
            """)
    List<Documento> vencidos(@Param("hoy") LocalDate hoy, Pageable limite);

    @EntityGraph(attributePaths = "responsable")
    @Query("""
            select d from Documento d
            where d.activo = true and d.estado <> com.adminpro.documentos.entity.EstadoDocumento.FINALIZADO
              and d.responsable.activo = false
            order by d.consecutivo
            """)
    List<Documento> conResponsableInactivo(Pageable limite);

    List<ReferenciaConsecutivo> findByIdIn(Collection<Long> ids);
}
