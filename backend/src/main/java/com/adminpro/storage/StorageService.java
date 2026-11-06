package com.adminpro.storage;

import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;

/**
 * Almacenamiento de adjuntos. La aplicación solo conoce esta interfaz, así que migrar
 * a MinIO o S3 es agregar otra implementación sin tocar el módulo de documentos.
 */
public interface StorageService {

    void guardar(UUID clave, byte[] contenido);

    Optional<Resource> abrir(UUID clave);

    boolean existe(UUID clave);

    void eliminar(UUID clave);
}
