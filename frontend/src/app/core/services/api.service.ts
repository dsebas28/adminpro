import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { PageResponse } from '../../models/api';
import {
  Actividad,
  Adjunto,
  Alerta,
  Catalogo,
  ConteoEstado,
  Documento,
  DocumentoItem,
  DocumentoRequest,
  Empleado,
  EmpleadoRequest,
  HistorialDocumento,
  RegistroAuditoria,
  Resumen,
  Rol,
  Usuario,
} from '../../models/negocio';

const API = '/api/v1';

export type TipoCatalogo = 'departamentos' | 'cargos' | 'categorias-documento';

/** Las llamadas al backend, con los mismos nombres del contrato de API. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Catálogos
  catalogos(tipo: TipoCatalogo, parametros: HttpParams): Observable<PageResponse<Catalogo>> {
    return this.http.get<PageResponse<Catalogo>>(`${API}/${tipo}`, { params: parametros });
  }

  /** Opciones de un select: solo activos. 100 es el máximo que admite la API. */
  opciones(tipo: TipoCatalogo): Observable<Catalogo[]> {
    const params = new HttpParams().set('activo', true).set('size', 100).set('sort', 'nombre,asc');
    return this.catalogos(tipo, params).pipe(map((pagina) => pagina.content));
  }

  crearCatalogo(tipo: TipoCatalogo, datos: Partial<Catalogo>): Observable<Catalogo> {
    return this.http.post<Catalogo>(`${API}/${tipo}`, datos);
  }

  editarCatalogo(tipo: TipoCatalogo, id: number, datos: Partial<Catalogo>): Observable<Catalogo> {
    return this.http.put<Catalogo>(`${API}/${tipo}/${id}`, datos);
  }

  cambiarCatalogo(tipo: TipoCatalogo, id: number, activar: boolean): Observable<Catalogo> {
    return this.http.patch<Catalogo>(`${API}/${tipo}/${id}/${activar ? 'activar' : 'desactivar'}`, null);
  }

  // Empleados
  empleados(parametros: HttpParams): Observable<PageResponse<Empleado>> {
    return this.http.get<PageResponse<Empleado>>(`${API}/empleados`, { params: parametros });
  }

  empleadosActivos(): Observable<Empleado[]> {
    const params = new HttpParams().set('activo', true).set('size', 100).set('sort', 'apellidos,asc');
    return this.empleados(params).pipe(map((pagina) => pagina.content));
  }

  empleado(id: number): Observable<Empleado> {
    return this.http.get<Empleado>(`${API}/empleados/${id}`);
  }

  crearEmpleado(datos: EmpleadoRequest): Observable<Empleado> {
    return this.http.post<Empleado>(`${API}/empleados`, datos);
  }

  editarEmpleado(id: number, datos: EmpleadoRequest): Observable<Empleado> {
    return this.http.put<Empleado>(`${API}/empleados/${id}`, datos);
  }

  cambiarEmpleado(id: number, activar: boolean): Observable<Empleado> {
    return this.http.patch<Empleado>(`${API}/empleados/${id}/${activar ? 'activar' : 'desactivar'}`, null);
  }

  historialEmpleado(id: number): Observable<PageResponse<RegistroAuditoria>> {
    return this.http.get<PageResponse<RegistroAuditoria>>(`${API}/empleados/${id}/historial`, {
      params: new HttpParams().set('size', 50),
    });
  }

  // Documentos
  documentos(parametros: HttpParams): Observable<PageResponse<DocumentoItem>> {
    return this.http.get<PageResponse<DocumentoItem>>(`${API}/documentos`, { params: parametros });
  }

  documento(id: number): Observable<Documento> {
    return this.http.get<Documento>(`${API}/documentos/${id}`);
  }

  crearDocumento(datos: DocumentoRequest): Observable<Documento> {
    return this.http.post<Documento>(`${API}/documentos`, datos);
  }

  editarDocumento(id: number, datos: DocumentoRequest): Observable<Documento> {
    return this.http.put<Documento>(`${API}/documentos/${id}`, datos);
  }

  transitar(id: number, accion: string, version: number, comentario?: string): Observable<Documento> {
    return this.http.post<Documento>(`${API}/documentos/${id}/${accion}`, { version, comentario });
  }

  cambiarDocumento(id: number, activar: boolean): Observable<Documento> {
    return this.http.patch<Documento>(`${API}/documentos/${id}/${activar ? 'activar' : 'desactivar'}`, null);
  }

  historialDocumento(id: number): Observable<PageResponse<HistorialDocumento>> {
    return this.http.get<PageResponse<HistorialDocumento>>(`${API}/documentos/${id}/historial`);
  }

  agregarAdjunto(id: number, archivo: File): Observable<Adjunto> {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return this.http.post<Adjunto>(`${API}/documentos/${id}/adjuntos`, datos);
  }

  descargarAdjunto(id: number, adjuntoId: number): Observable<Blob> {
    return this.http.get(`${API}/documentos/${id}/adjuntos/${adjuntoId}/descarga`, { responseType: 'blob' });
  }

  eliminarAdjunto(id: number, adjuntoId: number): Observable<void> {
    return this.http.delete<void>(`${API}/documentos/${id}/adjuntos/${adjuntoId}`);
  }

  // Usuarios
  usuarios(parametros: HttpParams): Observable<PageResponse<Usuario>> {
    return this.http.get<PageResponse<Usuario>>(`${API}/usuarios`, { params: parametros });
  }

  roles(): Observable<Rol[]> {
    return this.http.get<Rol[]>(`${API}/roles`);
  }

  crearUsuario(datos: object): Observable<Usuario> {
    return this.http.post<Usuario>(`${API}/usuarios`, datos);
  }

  editarUsuario(id: number, datos: object): Observable<Usuario> {
    return this.http.put<Usuario>(`${API}/usuarios/${id}`, datos);
  }

  cambiarUsuario(id: number, activar: boolean): Observable<Usuario> {
    return this.http.patch<Usuario>(`${API}/usuarios/${id}/${activar ? 'activar' : 'desactivar'}`, null);
  }

  restablecerPassword(id: number, passwordTemporal: string): Observable<void> {
    return this.http.post<void>(`${API}/usuarios/${id}/restablecer-password`, { passwordTemporal });
  }

  // Auditoría
  auditoria(parametros: HttpParams): Observable<PageResponse<RegistroAuditoria>> {
    return this.http.get<PageResponse<RegistroAuditoria>>(`${API}/auditoria`, { params: parametros });
  }

  // Dashboard
  resumen(): Observable<Resumen> {
    return this.http.get<Resumen>(`${API}/dashboard/resumen`);
  }

  documentosPorEstado(): Observable<ConteoEstado[]> {
    return this.http.get<ConteoEstado[]>(`${API}/dashboard/documentos-por-estado`);
  }

  documentosPorVencer(): Observable<PageResponse<DocumentoItem>> {
    return this.http.get<PageResponse<DocumentoItem>>(`${API}/dashboard/documentos-por-vencer`, {
      params: new HttpParams().set('size', 6),
    });
  }

  actividadReciente(): Observable<Actividad[]> {
    return this.http.get<Actividad[]>(`${API}/dashboard/actividad-reciente`, {
      params: new HttpParams().set('limite', 8),
    });
  }

  alertas(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(`${API}/dashboard/alertas`);
  }
}
