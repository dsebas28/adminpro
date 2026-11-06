package com.adminpro.documentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.adminpro.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

class DocumentosIT extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.4\n%prueba\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Test
    void crearAsignaConsecutivoDelAnioYEstadoRegistrado() throws Exception {
        int anio = LocalDate.now().getYear();

        crear(AUXILIAR, "Documento nuevo")
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.consecutivo").value(org.hamcrest.Matchers.matchesPattern("DOC-" + anio + "-\\d{6}")))
                .andExpect(jsonPath("$.estado").value("REGISTRADO"))
                .andExpect(jsonPath("$.accionesDisponibles", hasItem("EDITAR")))
                .andExpect(jsonPath("$.accionesDisponibles", hasItem("ENVIAR")));
    }

    @Test
    void flujoCompletoDejaLaLineaDeTiempo() throws Exception {
        long id = idDe(crear(AUXILIAR, "Flujo completo"));
        adjuntar(AUXILIAR, id, "contrato.pdf", PDF).andExpect(status().isCreated());

        transitar(AUXILIAR, id, "enviar", null).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        transitar(SUPERVISOR, id, "iniciar-revision", null).andExpect(jsonPath("$.estado").value("EN_REVISION"));
        transitar(SUPERVISOR, id, "aprobar", null).andExpect(jsonPath("$.estado").value("APROBADO"));
        transitar(SUPERVISOR, id, "finalizar", null)
                .andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.accionesDisponibles").isEmpty());

        mvc.perform(get("/api/v1/documentos/" + id + "/historial").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].estadoNuevo").value("FINALIZADO"))
                .andExpect(jsonPath("$.content[0].usuario.nombre").value("Julián Ramírez"));
    }

    @Test
    void enviarSinAdjuntoEs409() throws Exception {
        long id = idDe(crear(AUXILIAR, "Sin adjunto"));

        transitar(AUXILIAR, id, "enviar", null).andExpect(status().isConflict());
    }

    @Test
    void unaTransicionFueraDeOrdenEs409() throws Exception {
        long id = idDe(crear(AUXILIAR, "Fuera de orden"));

        transitar(SUPERVISOR, id, "aprobar", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO"));
    }

    @Test
    void quienCreaUnDocumentoNoPuedeAprobarlo() throws Exception {
        long id = idDe(crear(SUPERVISOR, "Separación de funciones"));
        adjuntar(SUPERVISOR, id, "a.pdf", PDF);
        transitar(SUPERVISOR, id, "enviar", null);
        transitar(SUPERVISOR, id, "iniciar-revision", null)
                .andExpect(jsonPath("$.accionesDisponibles", not(hasItem("APROBAR"))));

        transitar(SUPERVISOR, id, "aprobar", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEPARACION_FUNCIONES"));

        transitar(ADMIN, id, "aprobar", null).andExpect(status().isOk());
    }

    @Test
    void devolverExigeComentarioYVuelveAPendiente() throws Exception {
        long id = enRevision("Devolución");

        transitar(SUPERVISOR, id, "devolver", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("comentario"));
        transitar(SUPERVISOR, id, "devolver", "   ").andExpect(status().isBadRequest());

        transitar(SUPERVISOR, id, "devolver", "Falta la firma")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        mvc.perform(get("/api/v1/documentos/" + id + "/historial").header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR)))
                .andExpect(jsonPath("$.content[0].comentario").value("Falta la firma"));
    }

    @Test
    void enRevisionLosDatosYAdjuntosQuedanCongelados() throws Exception {
        long id = enRevision("Congelado");

        editar(AUXILIAR, id, "Otro título", version(id)).andExpect(status().isConflict());
        adjuntar(AUXILIAR, id, "b.pdf", PDF).andExpect(status().isConflict());
    }

    @Test
    void versionDesactualizadaEs409() throws Exception {
        long id = idDe(crear(AUXILIAR, "Versión"));
        long version = version(id);
        editar(AUXILIAR, id, "Primera edición", version).andExpect(status().isOk());

        editar(AUXILIAR, id, "Segunda edición", version)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_DESACTUALIZADA"));
    }

    @Test
    void desactivarBloqueaElFlujoYSePuedeRevertir() throws Exception {
        long id = idDe(crear(AUXILIAR, "Desactivar"));
        adjuntar(AUXILIAR, id, "a.pdf", PDF);

        mvc.perform(patch("/api/v1/documentos/" + id + "/desactivar").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesDisponibles").isEmpty());
        transitar(AUXILIAR, id, "enviar", null).andExpect(status().isConflict());

        mvc.perform(patch("/api/v1/documentos/" + id + "/activar").header(HttpHeaders.AUTHORIZATION, tokenDe(ADMIN)))
                .andExpect(status().isOk());
        transitar(AUXILIAR, id, "enviar", null).andExpect(status().isOk());
    }

    @Test
    void fechasInconsistentesSeñalanElCampo() throws Exception {
        mvc.perform(post("/api/v1/documentos").header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Fechas", "2026-05-10", "2026-05-01", empleadoActivo(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fechaVencimiento"));
    }

    @Test
    void responsableInactivoSeRechaza() throws Exception {
        Long inactivo = jdbc.queryForObject("select id from empleados where cedula = '1000200325'", Long.class);

        mvc.perform(post("/api/v1/documentos").header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Inactivo", "2026-05-10", null, inactivo, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("responsableId"));
    }

    @Test
    void filtrosPorNumeroTextoEstadoYFecha() throws Exception {
        String token = tokenDe(CONSULTA);

        mvc.perform(get("/api/v1/documentos").param("consecutivo", "doc-2025-000002").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Reglamento interno de trabajo"));
        mvc.perform(get("/api/v1/documentos").param("q", "POLIZA").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/documentos").param("fechaDesde", "2025-01-01").param("fechaHasta", "2025-12-31")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(get("/api/v1/documentos").param("estado", "NO_EXISTE").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elConsecutivoNoSeRepiteNiDejaHuecosConPeticionesSimultaneas() throws Exception {
        int hilos = 24;
        String token = tokenDe(AUXILIAR);
        Long empleado = empleadoActivo();
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> resultados = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(hilos)) {
            for (int i = 0; i < hilos; i++) {
                int numero = i;
                resultados.add(pool.submit(() -> {
                    salida.await();
                    String cuerpo = mvc.perform(post("/api/v1/documentos").header(HttpHeaders.AUTHORIZATION, token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(cuerpo("Concurrente " + numero, "2026-05-10", null, empleado, null)))
                            .andExpect(status().isCreated())
                            .andReturn().getResponse().getContentAsString();
                    return JsonPath.read(cuerpo, "$.consecutivo");
                }));
            }
            salida.countDown();

            List<Integer> numeros = new ArrayList<>();
            for (Future<String> resultado : resultados) {
                String consecutivo = resultado.get();
                numeros.add(Integer.valueOf(consecutivo.substring(consecutivo.lastIndexOf('-') + 1)));
            }
            assertThat(new HashSet<>(numeros)).hasSize(hilos);
            int minimo = numeros.stream().mapToInt(Integer::intValue).min().orElseThrow();
            int maximo = numeros.stream().mapToInt(Integer::intValue).max().orElseThrow();
            assertThat(maximo - minimo + 1).isEqualTo(hilos);
        }
    }

    @Test
    void aceptaLosTiposPermitidosSegunSuContenido() throws Exception {
        long id = idDe(crear(AUXILIAR, "Tipos"));

        adjuntar(AUXILIAR, id, "a.pdf", PDF).andExpect(jsonPath("$.tipoMime").value("application/pdf"));
        adjuntar(AUXILIAR, id, "b.png", PNG).andExpect(jsonPath("$.tipoMime").value("image/png"));
        adjuntar(AUXILIAR, id, "c.docx", office("word/document.xml"))
                .andExpect(jsonPath("$.tipoMime").value(org.hamcrest.Matchers.containsString("wordprocessingml")));
        adjuntar(AUXILIAR, id, "d.xlsx", office("xl/workbook.xml"))
                .andExpect(jsonPath("$.tipoMime").value(org.hamcrest.Matchers.containsString("spreadsheetml")));
    }

    @Test
    void unArchivoDisfrazadoSeRechazaAunqueDigaSerPdf() throws Exception {
        long id = idDe(crear(AUXILIAR, "Disfrazado"));
        byte[] ejecutable = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};

        adjuntar(AUXILIAR, id, "factura.pdf", ejecutable)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("TIPO_NO_SOPORTADO"));
        adjuntar(AUXILIAR, id, "pagina.pdf", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void unZipCualquieraNoPasaComoDocx() throws Exception {
        long id = idDe(crear(AUXILIAR, "Zip"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("virus.exe"));
            zip.write(1);
            zip.closeEntry();
        }

        adjuntar(AUXILIAR, id, "informe.docx", bytes.toByteArray()).andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void archivoVacioEs400() throws Exception {
        long id = idDe(crear(AUXILIAR, "Vacío"));

        adjuntar(AUXILIAR, id, "a.pdf", new byte[0]).andExpect(status().isBadRequest());
    }

    @Test
    void unNombreConRutaNoEscapaDelDirectorio() throws Exception {
        long id = idDe(crear(AUXILIAR, "Ruta"));

        adjuntar(AUXILIAR, id, "../../../etc/passwd.pdf", PDF)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreOriginal").value("passwd.pdf"));
    }

    @Test
    void elUndecimoAdjuntoEs409() throws Exception {
        long id = idDe(crear(AUXILIAR, "Máximo"));
        for (int i = 0; i < 10; i++) {
            adjuntar(AUXILIAR, id, i + ".pdf", PDF).andExpect(status().isCreated());
        }

        adjuntar(AUXILIAR, id, "11.pdf", PDF).andExpect(status().isConflict());
    }

    @Test
    void laDescargaFuerzaAdjuntoYConservaElContenido() throws Exception {
        long id = idDe(crear(AUXILIAR, "Descarga"));
        long adjunto = idAdjunto(adjuntar(AUXILIAR, id, "contrato año 2026.pdf", PDF));

        byte[] descargado = mvc.perform(get(rutaAdjunto(id, adjunto) + "/descarga")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(descargado).isEqualTo(PDF);
    }

    @Test
    void unAdjuntoNoSePuedePedirPorOtroDocumento() throws Exception {
        long id = idDe(crear(AUXILIAR, "Dueño"));
        long otro = idDe(crear(AUXILIAR, "Otro"));
        long adjunto = idAdjunto(adjuntar(AUXILIAR, id, "a.pdf", PDF));

        mvc.perform(get(rutaAdjunto(otro, adjunto) + "/descarga").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnAdjuntoEsBorradoLogico() throws Exception {
        long id = idDe(crear(AUXILIAR, "Eliminar"));
        long adjunto = idAdjunto(adjuntar(AUXILIAR, id, "a.pdf", PDF));

        mvc.perform(delete(rutaAdjunto(id, adjunto)).header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/documentos/" + id + "/adjuntos").header(HttpHeaders.AUTHORIZATION, tokenDe(AUXILIAR)))
                .andExpect(jsonPath("$.length()").value(0));
        assertThat(jdbc.queryForObject("select count(*) from documento_adjuntos where id = ?", Long.class, adjunto))
                .isEqualTo(1);
    }

    @Test
    void losAdjuntosDeLosDatosDePruebaSePuedenDescargar() throws Exception {
        Long adjunto = jdbc.queryForObject("select min(id) from documento_adjuntos", Long.class);
        Long documento = jdbc.queryForObject("select documento_id from documento_adjuntos where id = ?", Long.class, adjunto);

        mvc.perform(get(rutaAdjunto(documento, adjunto) + "/descarga").header(HttpHeaders.AUTHORIZATION, tokenDe(CONSULTA)))
                .andExpect(status().isOk());
    }

    private long enRevision(String titulo) throws Exception {
        long id = idDe(crear(AUXILIAR, titulo));
        adjuntar(AUXILIAR, id, "a.pdf", PDF);
        transitar(AUXILIAR, id, "enviar", null);
        transitar(SUPERVISOR, id, "iniciar-revision", null);
        return id;
    }

    private String rutaAdjunto(long documento, long adjunto) {
        return "/api/v1/documentos/" + documento + "/adjuntos/" + adjunto;
    }

    private ResultActions crear(String correo, String titulo) throws Exception {
        return mvc.perform(post("/api/v1/documentos").header(HttpHeaders.AUTHORIZATION, tokenDe(correo))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(titulo, "2026-05-10", "2027-05-10", empleadoActivo(), null)));
    }

    private ResultActions editar(String correo, long id, String titulo, long version) throws Exception {
        return mvc.perform(put("/api/v1/documentos/" + id).header(HttpHeaders.AUTHORIZATION, tokenDe(correo))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(titulo, "2026-05-10", null, empleadoActivo(), version)));
    }

    private String cuerpo(String titulo, String fecha, String vence, Long responsable, Long version) {
        Long categoria = jdbc.queryForObject("select min(id) from categorias_documento", Long.class);
        return """
                {"titulo":"%s","categoriaId":%d,"responsableId":%d,"fechaDocumento":"%s",
                 "fechaVencimiento":%s,"version":%s}
                """.formatted(titulo, categoria, responsable, fecha, vence == null ? "null" : '"' + vence + '"', version);
    }

    private Long empleadoActivo() {
        return jdbc.queryForObject("select id from empleados where cedula = '1000200322'", Long.class);
    }

    private ResultActions transitar(String correo, long id, String accion, String comentario) throws Exception {
        String texto = comentario == null ? "null" : '"' + comentario + '"';
        return mvc.perform(post("/api/v1/documentos/" + id + "/" + accion)
                .header(HttpHeaders.AUTHORIZATION, tokenDe(correo))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":%d,\"comentario\":%s}".formatted(version(id), texto)));
    }

    private ResultActions adjuntar(String correo, long id, String nombre, byte[] contenido) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
        return mvc.perform(multipart("/api/v1/documentos/" + id + "/adjuntos").file(archivo)
                .header(HttpHeaders.AUTHORIZATION, tokenDe(correo)));
    }

    private long version(long id) {
        return jdbc.queryForObject("select version from documentos where id = ?", Long.class, id);
    }

    private long idDe(ResultActions respuesta) throws Exception {
        return ((Number) JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long idAdjunto(ResultActions respuesta) throws Exception {
        return idDe(respuesta);
    }

    private byte[] office(String marcador) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String entrada : List.of("[Content_Types].xml", marcador)) {
                zip.putNextEntry(new ZipEntry(entrada));
                zip.write("<xml/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
