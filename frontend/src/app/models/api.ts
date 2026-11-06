/** Formato único de los listados. Igual a PageResponse del backend. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErrorCampo {
  field: string;
  message: string;
}

/** Errores de la API en formato Problem Details (RFC 9457). */
export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  code: string;
  errors?: ErrorCampo[];
}
