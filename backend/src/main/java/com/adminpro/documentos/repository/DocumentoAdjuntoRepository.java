package com.adminpro.documentos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.adminpro.documentos.entity.DocumentoAdjunto;

public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {

    List<DocumentoAdjunto> findByDocumentoIdAndActivoTrueOrderById(Long documentoId);

    List<DocumentoAdjunto> findByActivoTrue();

    Optional<DocumentoAdjunto> findByIdAndDocumentoIdAndActivoTrue(Long id, Long documentoId);

    long countByDocumentoIdAndActivoTrue(Long documentoId);
}
