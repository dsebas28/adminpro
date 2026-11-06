package com.adminpro.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import com.adminpro.exception.ApiException;
import com.adminpro.exception.TipoError;

/**
 * Determina el tipo real de un archivo por su contenido. La extensión y el Content-Type
 * los controla quien sube el archivo, así que no se usan. Tika reconoce PDF e imágenes
 * por sus primeros bytes; DOCX y XLSX son archivos ZIP, y se distinguen por su contenido.
 */
@Component
public class TipoArchivoValidator {

    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final Set<String> DIRECTOS = Set.of("application/pdf", "image/png", "image/jpeg", "image/webp");
    private static final Map<String, String> MARCADORES_OFFICE = Map.of("word/document.xml", DOCX, "xl/workbook.xml", XLSX);
    private static final String TIPOS_OPC = "[Content_Types].xml";

    private final Tika tika = new Tika();

    public String tipoReal(byte[] contenido) {
        String detectado = tika.detect(contenido);
        if (DIRECTOS.contains(detectado)) {
            return detectado;
        }
        if (esZip(detectado)) {
            String office = tipoOffice(contenido);
            if (office != null) {
                return office;
            }
        }
        throw new ApiException(TipoError.TIPO_NO_SOPORTADO,
                "Solo se permiten archivos PDF, DOCX, XLSX e imágenes PNG, JPEG o WEBP");
    }

    private boolean esZip(String tipo) {
        return "application/zip".equals(tipo) || tipo.startsWith("application/x-tika-ooxml")
                || DOCX.equals(tipo) || XLSX.equals(tipo);
    }

    /** Solo lee los nombres de las entradas, sin descomprimirlas. */
    private String tipoOffice(byte[] contenido) {
        Set<String> nombres = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                nombres.add(entrada.getName());
            }
        } catch (IOException e) {
            return null;
        }
        if (!nombres.contains(TIPOS_OPC)) {
            return null;
        }
        return MARCADORES_OFFICE.entrySet().stream()
                .filter(marcador -> nombres.contains(marcador.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
