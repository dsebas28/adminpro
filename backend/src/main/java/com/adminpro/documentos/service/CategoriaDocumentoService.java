package com.adminpro.documentos.service;

import org.springframework.stereotype.Service;

import com.adminpro.audit.service.AuditService;
import com.adminpro.common.catalogo.CatalogoService;
import com.adminpro.documentos.entity.CategoriaDocumento;
import com.adminpro.documentos.repository.CategoriaDocumentoRepository;
import com.adminpro.documentos.repository.DocumentoRepository;

@Service
public class CategoriaDocumentoService extends CatalogoService<CategoriaDocumento> {

    private final DocumentoRepository documentos;

    public CategoriaDocumentoService(CategoriaDocumentoRepository repositorio, DocumentoRepository documentos,
            AuditService auditoria) {
        super(repositorio, auditoria, "CATEGORIA_DOCUMENTO", "categoría");
        this.documentos = documentos;
    }

    @Override
    protected CategoriaDocumento nueva(String nombre, String descripcion) {
        return new CategoriaDocumento(nombre, descripcion);
    }

    @Override
    protected boolean enUso(Long id) {
        return documentos.existsByCategoria_IdAndActivoTrue(id);
    }
}
