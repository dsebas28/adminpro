/** Permisos del MVP. Deben coincidir con la migración V6 del backend. */
export type Permiso =
  | 'DASHBOARD_LEER'
  | 'EMPLEADOS_LEER'
  | 'EMPLEADOS_ESCRIBIR'
  | 'EMPLEADOS_DESACTIVAR'
  | 'CATALOGOS_ESCRIBIR'
  | 'DOCUMENTOS_LEER'
  | 'DOCUMENTOS_ESCRIBIR'
  | 'DOCUMENTOS_APROBAR'
  | 'DOCUMENTOS_DESACTIVAR'
  | 'USUARIOS_LEER'
  | 'USUARIOS_ESCRIBIR'
  | 'USUARIOS_DESACTIVAR'
  | 'AUDITORIA_LEER';

export interface UsuarioActual {
  id: number;
  correo: string;
  nombreVisible: string;
  rol: string;
  permisos: Permiso[];
  debeCambiarPassword: boolean;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
  usuario: UsuarioActual;
}

export interface LoginRequest {
  correo: string;
  password: string;
}

export interface CambiarPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}
