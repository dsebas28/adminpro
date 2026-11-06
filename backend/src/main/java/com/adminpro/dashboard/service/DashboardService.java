package com.adminpro.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adminpro.audit.entity.Auditoria;
import com.adminpro.audit.repository.AuditoriaRepository;
import com.adminpro.common.PageResponse;
import com.adminpro.common.RefResponse;
import com.adminpro.common.Zona;
import com.adminpro.dashboard.dto.DashboardDtos.ActividadResponse;
import com.adminpro.dashboard.dto.DashboardDtos.AlertaResponse;
import com.adminpro.dashboard.dto.DashboardDtos.ConteoEstadoResponse;
import com.adminpro.dashboard.dto.DashboardDtos.ResumenResponse;
import com.adminpro.documentos.dto.DocumentoDtos.DocumentoListItemResponse;
import com.adminpro.documentos.entity.Documento;
import com.adminpro.documentos.entity.EstadoDocumento;
import com.adminpro.documentos.repository.DocumentoRepository;
import com.adminpro.empleados.entity.Empleado;
import com.adminpro.empleados.repository.EmpleadoRepository;
import com.adminpro.exception.ApiException;
import com.adminpro.usuarios.entity.Usuario;
import com.adminpro.usuarios.repository.UsuarioRepository;

/** Todo se calcula en la base de datos: el frontend solo muestra los resultados. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int MAX_ACTIVIDAD = 50;
    private static final int MAX_ALERTAS_POR_TIPO = 10;
    private static final String DOCUMENTO = "DOCUMENTO";
    private static final String EMPLEADO = "EMPLEADO";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpleadoRepository empleados;
    private final DocumentoRepository documentos;
    private final AuditoriaRepository auditoria;
    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final int diasPorVencer;

    public DashboardService(EmpleadoRepository empleados, DocumentoRepository documentos,
            AuditoriaRepository auditoria, UsuarioRepository usuarios, Clock reloj,
            @Value("${adminpro.documentos.dias-por-vencer}") int diasPorVencer) {
        this.empleados = empleados;
        this.documentos = documentos;
        this.auditoria = auditoria;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.diasPorVencer = diasPorVencer;
    }

    public ResumenResponse resumen() {
        LocalDate hoy = Zona.hoy(reloj);
        return new ResumenResponse(empleados.countByActivo(true), empleados.countByActivo(false),
                documentos.countByActivoTrue(), documentos.contarPorVencer(hoy, hoy.plusDays(diasPorVencer)),
                documentos.contarVencidos(hoy));
    }

    /** Incluye los estados sin documentos, para que el gráfico siempre muestre el flujo completo. */
    public List<ConteoEstadoResponse> documentosPorEstado() {
        Map<EstadoDocumento, Long> conteo = documentos.contarPorEstado().stream()
                .collect(Collectors.toMap(DocumentoRepository.ConteoEstado::getEstado,
                        DocumentoRepository.ConteoEstado::getTotal));
        return Arrays.stream(EstadoDocumento.values())
                .map(estado -> new ConteoEstadoResponse(estado, conteo.getOrDefault(estado, 0L)))
                .toList();
    }

    public PageResponse<DocumentoListItemResponse> documentosPorVencer(Integer dias, Pageable paginacion) {
        int ventana = dias == null ? diasPorVencer : dias;
        if (ventana < 0 || ventana > 365) {
            throw ApiException.campo("dias", "Debe estar entre 0 y 365");
        }
        LocalDate hoy = Zona.hoy(reloj);
        Pageable orden = PageRequest.of(paginacion.getPageNumber(), paginacion.getPageSize(),
                Sort.by("fechaVencimiento"));
        return PageResponse.of(documentos.porVencer(hoy, hoy.plusDays(ventana), orden), d -> aItem(d, hoy));
    }

    /**
     * Toda persona con sesión ve esta actividad, incluso sin permiso de auditoría. Por eso
     * solo incluye documentos y empleados, y nunca los valores anteriores o nuevos.
     */
    public List<ActividadResponse> actividadReciente(Integer limite) {
        int tamano = Math.clamp(limite == null ? 10 : limite, 1, MAX_ACTIVIDAD);
        List<Auditoria> eventos = auditoria.findByEntidadIn(Set.of(DOCUMENTO, EMPLEADO),
                PageRequest.of(0, tamano, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        Map<Long, String> autores = usuarios.findAllById(eventos.stream().map(Auditoria::getCreatedBy)
                        .filter(Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombreVisible));
        Map<Long, String> consecutivos = documentos.findByIdIn(idsDe(eventos, DOCUMENTO)).stream()
                .collect(Collectors.toMap(DocumentoRepository.ReferenciaConsecutivo::getId,
                        DocumentoRepository.ReferenciaConsecutivo::getConsecutivo));
        Map<Long, String> nombres = empleados.findAllById(idsDe(eventos, EMPLEADO)).stream()
                .collect(Collectors.toMap(Empleado::getId, Empleado::nombreCompleto, (a, b) -> a));

        return eventos.stream()
                .map(e -> new ActividadResponse(e.getAccion(), e.getEntidad(), e.getEntidadId(),
                        (DOCUMENTO.equals(e.getEntidad()) ? consecutivos : nombres).get(e.getEntidadId()),
                        autores.get(e.getCreatedBy()), e.getCreatedAt()))
                .toList();
    }

    public List<AlertaResponse> alertas() {
        LocalDate hoy = Zona.hoy(reloj);
        Pageable limite = PageRequest.of(0, MAX_ALERTAS_POR_TIPO);
        List<AlertaResponse> alertas = new ArrayList<>();
        for (Documento d : documentos.vencidos(hoy, limite)) {
            alertas.add(new AlertaResponse("DOCUMENTO_VENCIDO", "ALTA",
                    d.getConsecutivo() + " venció el " + FECHA.format(d.getFechaVencimiento()), DOCUMENTO, d.getId()));
        }
        for (Documento d : documentos.conResponsableInactivo(limite)) {
            alertas.add(new AlertaResponse("RESPONSABLE_INACTIVO", "MEDIA",
                    d.getConsecutivo() + " tiene como responsable a " + d.getResponsable().nombreCompleto()
                            + ", que está inactivo",
                    DOCUMENTO, d.getId()));
        }
        documentos.porVencer(hoy, hoy.plusDays(diasPorVencer),
                        PageRequest.of(0, MAX_ALERTAS_POR_TIPO, Sort.by("fechaVencimiento")))
                .forEach(d -> alertas.add(new AlertaResponse("DOCUMENTO_POR_VENCER", "BAJA",
                        d.getConsecutivo() + " vence el " + FECHA.format(d.getFechaVencimiento()), DOCUMENTO, d.getId())));
        return alertas;
    }

    private Set<Long> idsDe(List<Auditoria> eventos, String entidad) {
        return eventos.stream().filter(e -> entidad.equals(e.getEntidad())).map(Auditoria::getEntidadId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private DocumentoListItemResponse aItem(Documento d, LocalDate hoy) {
        return new DocumentoListItemResponse(d.getId(), d.getConsecutivo(), d.getTitulo(),
                new RefResponse(d.getCategoria().getId(), d.getCategoria().getNombre()),
                new RefResponse(d.getResponsable().getId(), d.getResponsable().nombreCompleto()),
                d.getEstado(), d.getFechaDocumento(), d.getFechaVencimiento(), d.estaVencido(hoy), d.isActivo());
    }
}
