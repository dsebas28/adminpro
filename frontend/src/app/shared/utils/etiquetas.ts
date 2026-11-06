import { EstadoDocumento } from '../../models/negocio';
import { TonoEstado } from '../components/estado';

export const ESTADOS: Record<EstadoDocumento, { texto: string; tono: TonoEstado }> = {
  REGISTRADO: { texto: 'Registrado', tono: 'neutro' },
  PENDIENTE: { texto: 'Pendiente', tono: 'aviso' },
  EN_REVISION: { texto: 'En revisión', tono: 'info' },
  APROBADO: { texto: 'Aprobado', tono: 'exito' },
  FINALIZADO: { texto: 'Finalizado', tono: 'neutro' },
};

export const LISTA_ESTADOS = Object.keys(ESTADOS) as EstadoDocumento[];

const ACCIONES: Record<string, string> = {
  CREAR: 'Creó',
  ACTUALIZAR: 'Editó',
  ACTIVAR: 'Activó',
  DESACTIVAR: 'Desactivó',
  CAMBIO_ESTADO: 'Cambió el estado de',
  ADJUNTO_AGREGADO: 'Agregó un adjunto a',
  ADJUNTO_ELIMINADO: 'Eliminó un adjunto de',
  LOGIN_OK: 'Inició sesión',
  LOGIN_FALLIDO: 'Intento de inicio de sesión fallido',
  CUENTA_BLOQUEADA: 'Cuenta bloqueada',
  LOGOUT: 'Cerró sesión',
  CAMBIO_PASSWORD: 'Cambió su contraseña',
  RESTABLECER_PASSWORD: 'Restableció la contraseña',
  REUTILIZACION_TOKEN: 'Posible robo de sesión detectado',
};

export const LISTA_ACCIONES_AUDITORIA = Object.keys(ACCIONES);

export function textoAccion(accion: string): string {
  return ACCIONES[accion] ?? accion;
}

const ENTIDADES: Record<string, string> = {
  USUARIO: 'Usuario',
  EMPLEADO: 'Empleado',
  DOCUMENTO: 'Documento',
  DEPARTAMENTO: 'Departamento',
  CARGO: 'Cargo',
  CATEGORIA_DOCUMENTO: 'Categoría',
};

export const LISTA_ENTIDADES = Object.keys(ENTIDADES);

export function textoEntidad(entidad: string): string {
  return ENTIDADES[entidad] ?? entidad;
}

const HISTORIAL_DOCUMENTO: Record<string, string> = {
  CREADO: 'Registró el documento',
  EDITADO: 'Editó los datos',
  ADJUNTO_AGREGADO: 'Agregó un adjunto',
  ADJUNTO_ELIMINADO: 'Eliminó un adjunto',
  DESACTIVADO: 'Desactivó el documento',
  REACTIVADO: 'Reactivó el documento',
};

export function textoHistorial(accion: string, anterior: EstadoDocumento | null, nuevo: EstadoDocumento | null): string {
  if (accion === 'CAMBIO_ESTADO' && anterior && nuevo) {
    return anterior === 'EN_REVISION' && nuevo === 'PENDIENTE'
      ? 'Devolvió el documento'
      : `Pasó a ${ESTADOS[nuevo].texto}`;
  }
  return HISTORIAL_DOCUMENTO[accion] ?? accion;
}

export function tamanoLegible(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;
}
