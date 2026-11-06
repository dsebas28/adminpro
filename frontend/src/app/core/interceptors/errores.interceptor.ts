import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { ProblemDetails } from '../../models/api';
import { ToastService } from '../services/toast.service';

/**
 * Solo atiende los errores que ninguna pantalla puede resolver por sí misma. Los errores
 * de validación y de conflicto (400, 409) los muestra el formulario, junto al campo.
 */
export const erroresInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const toasts = inject(ToastService);
  const router = inject(Router);

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const problema = error.error as ProblemDetails | null;
        if (error.status === 0) {
          toasts.error('No se pudo conectar con el servidor. Revisa tu conexión.');
        } else if (error.status >= 500) {
          toasts.error(problema?.detail ?? 'El servidor no pudo completar la operación.');
        } else if (problema?.code === 'CAMBIO_PASSWORD_REQUERIDO') {
          void router.navigateByUrl('/cambiar-password');
        } else if (problema?.code === 'SIN_PERMISO') {
          toasts.error(problema.detail);
        }
      }
      return throwError(() => error);
    }),
  );
};
