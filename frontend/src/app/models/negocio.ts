export interface Ref {
  id: number;
  nombre: string;
}

export interface Catalogo {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  version: number;
}

export interface Empleado {
  id: number;
  cedula: string;
  nombres: string;
  apellidos: string;
  correo: string;
  telefono: string | null;
  fechaIngreso: string;
  departamento: Ref;
  cargo: Ref;
  activo: boolean;
  version: number;
}

export interface EmpleadoRequest {
  cedula: string;
  nombres: string;
  apellidos: string;
  correo: string;
  telefono: string | null;
  fechaIngreso: string;
  departamentoId: number;
  cargoId: number;
  version?: number;
}

export type EstadoDocumento = 'REGISTRADO' | 'PENDIENTE' | 'EN_REVISION' | 'APROBADO' | 'FINALIZADO';

export type AccionDocumento = 'EDITAR' | 'ENVIAR' | 'INICIAR_REVISION' | 'APROBAR' | 'DEVOLVER' | 'FINALIZAR';

export interface DocumentoItem {
  id: number;
  consecutivo: string;
  titulo: string;
  categoria: Ref;
  responsable: Ref;
  estado: EstadoDocumento;
  fechaDocumento: string;
  fechaVencimiento: string | null;
  vencido: boolean;
  activo: boolean;
}

export interface Adjunto {
  id: number;
  nombreOriginal: string;
  tipoMime: string;
  tamanoBytes: number;
  createdAt: string;
}

export interface Documento extends DocumentoItem {
  descripcion: string | null;
  adjuntos: Adjunto[];
  accionesDisponibles: AccionDocumento[];
  version: number;
  createdAt: string;
}

export interface DocumentoRequest {
  titulo: string;
  descripcion: string | null;
  categoriaId: number;
  responsableId: number;
  fechaDocumento: string;
  fechaVencimiento: string | null;
  version?: number;
}

export interface HistorialDocumento {
  id: number;
  accion: string;
  estadoAnterior: EstadoDocumento | null;
  estadoNuevo: EstadoDocumento | null;
  comentario: string | null;
  usuario: Ref | null;
  createdAt: string;
}

export interface RegistroAuditoria {
  id: number;
  entidad: string;
  entidadId: number | null;
  accion: string;
  valorAnterior: Record<string, unknown> | null;
  valorNuevo: Record<string, unknown> | null;
  usuario: Ref | null;
  ip: string | null;
  createdAt: string;
}

export interface Usuario {
  id: number;
  correo: string;
  nombreVisible: string;
  rol: Ref;
  empleado: Ref | null;
  activo: boolean;
  bloqueadoHasta: string | null;
  ultimoAccesoAt: string | null;
  version: number;
}

export interface Rol {
  id: number;
  codigo: string;
  nombre: string;
  permisos: string[];
}

export interface Resumen {
  empleadosActivos: number;
  empleadosInactivos: number;
  documentosActivos: number;
  documentosPorVencer: number;
  documentosVencidos: number;
}

export interface ConteoEstado {
  estado: EstadoDocumento;
  total: number;
}

export interface Actividad {
  accion: string;
  entidad: string;
  entidadId: number | null;
  etiqueta: string | null;
  usuario: string | null;
  fecha: string;
}

export interface Alerta {
  tipo: string;
  severidad: 'ALTA' | 'MEDIA' | 'BAJA';
  mensaje: string;
  entidad: string;
  entidadId: number;
}
