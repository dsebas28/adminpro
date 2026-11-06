package com.adminpro.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Guarda los archivos en un directorio, que en Docker es un volumen persistente. */
@Service
public class LocalStorageService implements StorageService {

    private final Path raiz;

    public LocalStorageService(@Value("${adminpro.adjuntos.dir}") String directorio) {
        this.raiz = Path.of(directorio);
    }

    @Override
    public void guardar(UUID clave, byte[] contenido) {
        try {
            Files.createDirectories(raiz);
            Files.write(ruta(clave), contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el adjunto", e);
        }
    }

    @Override
    public Optional<Resource> abrir(UUID clave) {
        Path archivo = ruta(clave);
        return Files.isRegularFile(archivo) ? Optional.of(new FileSystemResource(archivo)) : Optional.empty();
    }

    @Override
    public boolean existe(UUID clave) {
        return Files.isRegularFile(ruta(clave));
    }

    @Override
    public void eliminar(UUID clave) {
        try {
            Files.deleteIfExists(ruta(clave));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el adjunto", e);
        }
    }

    /** La clave es un UUID generado por el sistema: el nombre del usuario nunca llega al disco. */
    private Path ruta(UUID clave) {
        return raiz.resolve(clave.toString());
    }
}
