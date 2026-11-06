package com.adminpro.documentos.service;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.adminpro.documentos.repository.DocumentoAdjuntoRepository;
import com.adminpro.storage.StorageService;

/**
 * Los datos de prueba registran adjuntos en la base de datos, pero el archivo no existe.
 * Este componente crea un PDF de ejemplo para cada uno, para que la descarga funcione.
 */
@Component
@Profile({"dev", "demo"})
public class AdjuntosEjemploRunner implements ApplicationRunner {

    private static final String PDF = """
            %PDF-1.4
            1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
            2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
            3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj
            4 0 obj<</Length 58>>stream
            BT /F1 18 Tf 72 700 Td (Documento de ejemplo AdminPro) Tj ET
            endstream endobj
            5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj
            trailer<</Root 1 0 R>>
            %%EOF
            """;

    private final DocumentoAdjuntoRepository adjuntos;
    private final StorageService almacenamiento;

    public AdjuntosEjemploRunner(DocumentoAdjuntoRepository adjuntos, StorageService almacenamiento) {
        this.adjuntos = adjuntos;
        this.almacenamiento = almacenamiento;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        byte[] contenido = PDF.getBytes(StandardCharsets.US_ASCII);
        adjuntos.findByActivoTrue().stream()
                .filter(adjunto -> !almacenamiento.existe(adjunto.getStorageKey()))
                .forEach(adjunto -> almacenamiento.guardar(adjunto.getStorageKey(), contenido));
    }
}
