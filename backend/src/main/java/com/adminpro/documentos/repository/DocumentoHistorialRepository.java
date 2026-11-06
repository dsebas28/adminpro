package com.adminpro.documentos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.adminpro.documentos.entity.DocumentoHistorial;

public interface DocumentoHistorialRepository extends JpaRepository<DocumentoHistorial, Long> {

    Page<DocumentoHistorial> findByDocumentoId(Long documentoId, Pageable paginacion);
}
